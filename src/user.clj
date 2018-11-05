(ns user ;; This namespace is loaded automatically by nREPL
  (:require [hawk.core :as hawk]
            [expound.alpha :as expound]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.io :as jio]
            [clojure.data.json :as json]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader :as reader]
            [figwheel-sidecar.repl-api :as ra]
            [clojure.pprint :as pprint]
            [clojure.spec.alpha :as s])
  (:import (java.net NetworkInterface InetAddress Inet4Address)
           (java.io File PushbackReader Writer)))

;; useful constants
(def user-dir (System/getProperty "user.dir"))

(def modules-cache ".js-modules.edn")

(def source-dir "src")

(def dev-env {:dev   {:edn "resources/dev/dev.edn" :cljs "src/env/dev.cljs"}
              :index {:edn "resources/dev/index.edn" :cljs "src/env/index.cljs"}
              :main  {:edn "resources/dev/main.edn" :cljs "src/env/expo/main.cljs"}})

(def package-json (json/read-str (slurp "package.json")))

(def js-deps (set (keys (merge (get package-json "dependencies")
                               (get package-json "devDependencies")))))
;; ............................................................................

(s/def ::token (s/and string? not-empty))
(s/def ::MAPBOX ::token)
(s/def ::FIREBASE_API_KEY ::token)
(s/def ::FIREBASE_AUTH_DOMAIN ::token)
(s/def ::FIREBASE_DATABASE_URL ::token)
(s/def ::FIREBASE_STORAGE_BUCKET ::token)

(s/def ::config (s/keys :req-un [::MAPBOX
                                 ::FIREBASE_API_KEY
                                 ::FIREBASE_AUTH_DOMAIN
                                 ::FIREBASE_DATABASE_URL
                                 ::FIREBASE_STORAGE_BUCKET]))

;; stop compilation if the required env vars are not provided
(defn- trim-config
  "takes a environment variables map and returns m with only the unqualified keys
   specified in spec. Throws an Error if m does not conform to spec"
  [config]
  (let [data (apply hash-map (rest (s/form ::config)))
        ks   (map #(keyword (name %))
                   (concat (:req-un data) (:opt-un data)))
        m    (select-keys config ks)]
    (if (s/valid? ::config m) m
      (throw (ex-info (expound/expound-str ::config m)
                      config)))))

;; ............................................................................

(defn spit-clojure
  "Oppsite of slurp. Opens f with writer, writes each item in coll, then
  closes f. Options passed to clojure.java.io/writer."
  {:added "1.2"}
  [f coll & options]
  (with-open [^Writer w (apply jio/writer f options)]
    (doseq [item coll]
      (.write w (with-out-str (pprint/pprint item))))))

(defn- read-clojure-dirty
  "read a clojure file into a sequence of edn objects.

  WARNING:
   This is using the clojure.tools.reader/read api which might execute
   your code. Although it should NOT do that there is afaik no guarantee
   of it. Only your own code will be read though so nothing should happen
   here that was not meant to happen in your original code base"
  [filename]
  (binding [reader/*alias-map*              identity
            reader/*default-data-reader-fn* (fn [tag v] v)
            reader/*read-eval*              false]
    (with-open [infile (PushbackReader. (io/reader filename))]
      (doall (for [_ (range)
                   :let [form (reader/read {:read-cond :allow
                                            :features  #{:cljs}
                                            :eof       ::eof}
                                           infile)]
                   :while (not= ::eof form)]
               form)))))

(defn- read-edn-seq
  "read an EDN file containing more than one element"
  [filename]
  (with-open [infile (PushbackReader. (io/reader filename))]
    (doall (for [_ (range)
                 :let [form (edn/read {:eof ::eof} infile)]
                 :while (not= ::eof form)]
             form))))

(defn- cljs-file?
  [^File file]
  (and (. file (isFile))
       (str/ends-with? (. file (getPath)) ".cljs")))

(defn enable-source-maps
  "patch the metro packager to use Clojurescript source maps"
  []
  (let [path "node_modules/metro/src/Server/index.js"
        content (slurp path)]
    (spit path (str/replace content #"match\(\/\\.map\$\/\)"
                                    "match(/main\\.*\\\\.map\\$/)"))
    (println "Source maps enabled.")))

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
        sip (standard-ip)]
    (or lip sip)))

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

(defn write-env-dev
  "First check the .expo/settings.json file to see what host is specified.  Then set the appropriate IP."
  []
  (let [hostname (.getHostName (InetAddress/getLocalHost))
        ip       (get-expo-ip)
        dev-ns   (walk/postwalk-replace {:tag/hostname hostname
                                         :tag/ip       ip}
                                        (read-edn-seq (get-in dev-env [:dev :edn])))]
    (spit-clojure (get-in dev-env [:dev :cljs]) dev-ns)))

(defn- normalize
  "removes specials characters from the asset filepath"
  [filepath]
  (-> filepath (str/replace "\\" "/")
               (str/replace "@2x" "")
               (str/replace "@3x" "")))

(defn- fake-require
  [module]
  (walk/postwalk-replace {::module module}
                         '(js/require ::module)))

;; NOTE: we assume that all js/require to assets are relative to main.js
;; HACK: we overwrite the js/require path to come from target/expo/env/index.js
;; as this is where all requires are indexed
(defn rebuild-env-index
  "prebuild the set of files that the metro packager requires in advance"
  [m]
  (let [devHost     (get-expo-ip)
        modules     (concat (flatten (vals m))
                            ["react" "react-native" "expo" "create-react-class"])
        modules-map (into {}
                      (for [module modules]
                        (let [path (if (str/starts-with? module "./resources")
                                     (str/replace (normalize module)
                                                  "./"
                                                  "../../../")
                                     module)]
                          [module (fake-require path)])))]
    (try
      (spit-clojure (get-in dev-env [:index :cljs])
                    (walk/postwalk-replace {:tag/js      (symbol "#js")
                                            :tag/modules modules-map
                                            :tag/ip      devHost}
                                           (read-edn-seq (get-in dev-env [:index :edn]))))
      (catch Exception e
        (println "Error: " e)))))

(defn- relative-path
  "transforms an absolute filename to a relative one. Relative to the current user.dir"
  [^File file]
  (str/replace (. file (getPath)) (str user-dir "/") ""))

(defn- form-require
  [form]
  (when (= 'js/require (first form))
    (second form)))

(defn- ns-requires
  [form]
  (when (= :require (first form))
    (for [dependency (rest form)
          :let [module (first (str/split (name (first dependency)) #"\."))]
          :when (contains? js-deps module)]
      module)))

(defn- modules
  [filename]
  (for [form    (read-clojure-dirty filename)
        subform (tree-seq seq? seq form)
        :when (seq? subform)
        :let [interop (ns-requires subform)
              raw     (form-require subform)]
        :when (or (some? interop) (some? raw))]
    (cons raw interop)))

(defn- required-modules
  "returns a vector of string with the names of the imported modules. Ignoring
   those that are commented out"
  [filename]
  (eduction cat (remove nil?) (distinct) (modules filename)))

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
  (let [old-modules (edn/read-string (slurp modules-cache))
        new-modules (on-file-change old-modules kind file)]
    (when (not= old-modules new-modules)
      (println "module requirements changed ... updating cache")
      (spit modules-cache new-modules)
      (rebuild-env-index new-modules))
    ctx))

(defn rebuild-modules-cache!
  "traverse src dir and fetch all modules"
  []
  (let [mapping (for [fs (file-seq (io/file source-dir))
                      :when (cljs-file? fs)
                      :let [filename (relative-path fs)]
                      :let [js-modules (required-modules filename)]
                      :when (not-empty js-modules)]
                  [filename (vec js-modules)])
        modules (into {} mapping)]
    (spit modules-cache (with-out-str (pprint/pprint modules)))))

;; Lein
(defn start-figwheel!
  "Start figwheel for one or more builds"
  [];& build-ids]
  (rebuild-modules-cache!)
  (rebuild-env-index (edn/read-string (slurp modules-cache)))
  (enable-source-maps)
  (io/copy (io/file "resources/dev/main.js")
           (io/file "main.js"))
  (write-env-dev)
  (io/copy (io/file (get-in dev-env [:main :edn]))
           (io/file (get-in dev-env [:main :cljs])))
  ;; Each file maybe corresponds to multiple modules.
  (hawk/watch! [{:paths   [source-dir]
                 :filter  (fn cljs-file??
                            [_ {:keys [file]}]
                            (cljs-file? file))
                 :handler hawk-handler}])
  (ra/start-figwheel! "main")
  (ra/cljs-repl))

(defn- prepare-env!
  []
  (doseq [file (map :cljs (vals dev-env))]
    (io/make-parents file))
  (doseq [file (map :cljs (vals dev-env))]
    (io/delete-file file :silently true)))

(defn- store-configs!
  []
  (let [env-vars (walk/keywordize-keys (into {} (System/getenv)))
        config   (trim-config env-vars)]
    (println "writing config to resources")
    (spit "resources/config.json" (json/write-str config))))

(defn -main
  [args]
  (store-configs!)
  (case args
    "--figwheel"
    (do (prepare-env!)
        (start-figwheel!))

    "--prepare-release"
    ;; assumes the dev env files were cleaned :)
    (do (prepare-env!)
        (io/copy (io/file (io/file "resources/release/main.edn"))
                 (io/file (get-in dev-env [:main :cljs]))))

    "--rebuild-modules"
    (rebuild-modules-cache!)))
