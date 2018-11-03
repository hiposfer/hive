(ns user
  (:require [figwheel-sidecar.repl-api :as ra]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader]]
            [clojure.walk :as walk]
            [clojure.java.io :as jio])
  (:import (java.net NetworkInterface InetAddress Inet4Address)
           (java.io File PushbackReader Writer)))
;; This namespace is loaded automatically by nREPL

(defn spit-clojure
  "Oppsite of slurp. Opens f with writer, writes each item in coll, then
  closes f. Options passed to clojure.java.io/writer."
  {:added "1.2"}
  [f coll & options]
  (with-open [^Writer w (apply jio/writer f options)]
    (doseq [item coll]
      (.write w (str item "\n")))))

(defn- read-clj
  "read a clojure file into a sequence of edn objects.

  WARNING:
   This is using the clojure.tools.reader/read api which might execute
   your code. Although it should NOT do that there is afaik no guarantee
   of it. Only your own code will be read though so nothing should happen
   here that was not meant to happen in your original code base"
  [filename]
  (binding [reader/*alias-map* identity
            reader/*default-data-reader-fn* (fn [tag v] v)
            reader/*read-eval* false]
    (with-open [infile (PushbackReader. (io/reader filename))]
      (->> (repeatedly #(reader/read {:read-cond :allow
                                      :features #{:cljs}
                                      :eof ::eof}
                                     infile))
           (take-while #(not= ::eof %))
           (doall)))))

(defn- cljs-file?
  [^File file]
  (and (. file (isFile)) (str/ends-with? (. file (getPath)) ".cljs")))

(defn enable-source-maps
  "patch the metro packager to use Clojurescript source maps"
  []
  (let [path "node_modules/metro/src/Server/index.js"
        content (slurp path)]
    (spit path (str/replace content #"match\(\/\\.map\$\/\)"
                                    "match(/main\\.*\\\\.map\\$/)"))
    (println "Source maps enabled.")))

(defn write-main-js
  "create a fake main.js file to make the metro packager happy"
  []
  (spit "main.js"
    "'use strict';
    // cljsbuild adds a preamble mentioning goog so hack around it
    window.goog = {
      provide: function() {},
      require: function() {},
      };
    require('./target/expo/env/index.js');"))

(defn get-expo-settings []
  (try
    (json/read-str (slurp ".expo/settings.json"))
    (catch Exception _
      nil)))

(def ip-validator #"\d+\.\d+\.\d+\.\d+")

(defn- linux-ip
  "attempts to retrieve the ip on linux OS"
  []
  (try
    (-> (Runtime/getRuntime)
        (.exec "ip route get 8.8.8.8 | head -n 1 | tr -s ' ' | cut -d ' ' -f 7")
        (.getInputStream)
        (slurp)
        (str/trim-newline)
        (re-matches ip-validator))
    (catch Exception _
      nil)))

(defn- standard-ip
  "attempts to check the lan ip through the Java API"
  []
  (cond
    (some #{(System/getProperty "os.name")} ["Mac OS X" "Windows 10"])
    (.getHostAddress (InetAddress/getLocalHost))

    :else
    (->> (NetworkInterface/getNetworkInterfaces)
         (enumeration-seq)
         (filter #(not (or (str/starts-with? (.getName %) "docker")
                           (str/starts-with? (.getName %) "br-"))))
         (map #(.getInterfaceAddresses %))
         (map
           (fn [ip]
             (seq (filter #(instance?
                             Inet4Address
                             (.getAddress %))
                          ip))))
         (remove nil?)
         (first)
         (filter #(instance?
                    Inet4Address
                    (.getAddress %)))
         (first)
         (.getAddress)
         (.getHostAddress))))

(defn get-lan-ip
  "fetch the ip of the computer that is available for expo app to communicate"
  []
  (let [lip (linux-ip)
        sip (standard-ip)
        ip  (or lip sip)]
    (println "using ip:" ip)
    ip))

(defn get-expo-ip []
  (let [expo-settings (get-expo-settings)]
    (if (nil? expo-settings)
      "localhost"
      (case (get expo-settings "hostType")
        "lan" (get-lan-ip)
        "localhost" "localhost"
        "tunnel" (throw (ex-info (str "Expo Setting tunnel doesn't work with figwheel."
                                      "Please set to LAN or Localhost.")
                                 {}))))))

(def env-dev '((ns env.dev)
               (def hostname ::hostname)
               (def ip ::ip)))

(defn write-env-dev
  "First check the .expo/settings.json file to see what host is specified.  Then set the appropriate IP."
  []
  (let [hostname (.getHostName (InetAddress/getLocalHost))
        ip       (get-expo-ip)
        dev-ns   (walk/postwalk-replace {::hostname hostname ::ip ip} env-dev)]
    (spit-clojure "env/dev/env/dev.cljs" dev-ns)))

(def env-index '((ns env.index)
                 ;; undo main.js goog preamble hack
                 (set! js/window.goog js/undefined)
                 ;; ...............................
                 (-> (js/require "figwheel-bridge")
                     (.withModules ::js-tag ::modules)
                     (.start "main" "expo" ::ip))))

(defn- normalize-filename
  "removes specials characters from the asset filepath"
  [filepath]
  (-> filepath (str/replace "\\" "/")
               (str/replace "@2x" "")
               (str/replace "@3x" "")))

(defn- fake-require
  [module]
  (walk/postwalk-replace {::module module}
                         '(js/require ::module)))

;; HACK: this functions pressumes the location of the running code to be
;; inside target/expo/env
(defn rebuild-env-index
  "prebuild the set of files that the metro packager requires in advance"
  [m]
  (let [devHost     (get-expo-ip)
        modules     (concat (flatten (vals m))
                            ["react" "react-native" "expo" "create-react-class"])
        entries     (concat
                      (for [module modules]
                        [module (fake-require module)])
        ;; HACK: concat assets second to overwrite the entries from modules
                      (for [file (file-seq (io/file "assets"))
                            :when (.isFile file)
                            :when (not (re-find #"DS_Store" (str file)))
                            :let [unix-path (->> file .toPath
                                                 .iterator
                                                 iterator-seq
                                                 (str/join "/"))
                                  normal    (normalize-filename unix-path)]]
                        [(str "./" normal)
                         (fake-require (str "../../../" normal))]))
        modules-map (into {} entries)]
    (try
      (spit-clojure "env/dev/env/index.cljs"
        (walk/postwalk-replace {::js-tag  (symbol "#js")
                                ::modules modules-map
                                ::ip devHost}
                               env-index))
      (catch Exception e
        (println "Error: " e)))))

(def user-dir (System/getProperty "user.dir"))

(def modules-file ".js-modules.edn")

;(rebuild-env-index (edn/read-string (slurp modules-file)))

(defn relative-path
  "transforms an absolute filename to a relative one. Relative to the current user.dir"
  [^File file]
  (str/replace (. file (getPath)) (str user-dir "/") ""))

(defn- required-modules
  "returns a vector of string with the names of the imported modules. Ignoring those
  that are commented out"
  [filename]
  (->> (read-clj filename)
       (tree-seq seq? seq) ;; read all function calls
       (filter seq?) ;; filter only function calls
       (filter #(= 'js/require (first %))) ;; only js/require
       (map second))) ;; only the required string

(defn- on-file-change
  [modules kind file]
  (let [filename (relative-path file)]
    (if (= :delete kind) ;; file is deleted
      (dissoc modules filename)
      ;; file created or modified
      (let [requires (required-modules filename)]
        (if (not-empty requires)
          (assoc modules filename requires)
          (dissoc modules filename))))))

(defn- hawk-handler
  [ctx {:keys [kind file] :as event}]
  (let [old-modules (edn/read-string (slurp modules-file))
        new-modules (on-file-change old-modules kind file)]
    (when (not= old-modules new-modules)
      (spit modules-file new-modules)
      (rebuild-env-index new-modules))
    ctx))

(defn rebuild-modules
  []
   ;; traverse src dir and fetch all modules
  (let [mapping (for [fs (file-seq (File. "src"))
                      :when (cljs-file? fs)
                      :let [filename (relative-path fs)]
                      :let [js-modules (required-modules filename)]
                      :when (not-empty js-modules)]
                  [filename (vec js-modules)])
        modules (into {} mapping)]
    (spit modules-file modules)))

  ;; Lein
(defn start-figwheel
  "Start figwheel for one or more builds"
  [];& build-ids]
  ;; delete previous file
  (when (.exists (File. ^String modules-file))
    (io/delete-file modules-file))
  (rebuild-modules)
  (rebuild-env-index (edn/read-string (slurp modules-file)))
  (enable-source-maps)
  (write-main-js)
  (write-env-dev)
  ;; Each file maybe corresponds to multiple modules.
  (hawk/watch! [{:paths   ["src"]
                 :filter  (fn cljs-file?? [_ {:keys [file]}]
                            (cljs-file? file))
                 :handler hawk-handler}])
  (ra/start-figwheel! "main")
  (ra/cljs-repl))

(defn -main
  [args]
  (case args
    "--figwheel"
    (start-figwheel)

    "--rebuild-modules"
    (rebuild-modules)

    (prn "You can run lein figwheel or lein rebuild-modules.")))
