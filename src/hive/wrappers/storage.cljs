(ns hive.wrappers.storage
  "wrapper around local storage database
  https://facebook.github.io/react-native/docs/asyncstorage.html"
  (:require [clojure.string :as str]
            [hive.foreigns :as fl]
            [cljs.reader :refer [read-string]]))

(defn- normalize
  "transforms a (hopefully namespaced) keyword into a string compatible
  with most rest-like pattern i.e. 'this/is/a/test'"
  [id]
  (let [k-ns   (str/split (namespace id) #"\." "/")
        k-name (name id)]
    (str/join "/" (conj k-ns k-name))))

(defn write!
  "takes a keyword an a value (clojure) and writes that value to
  AsyncStorage from ReactNative. Returns a promise according to the
  official documentation"
  [[{:keys [id value on-success on-error] :or {on-success identity on-error identity}}]]
  (let [path  (normalize id)]
    (.setItem fl/async-storage path (str value)
              (fn [error]
                (if error (on-error error)
                  (on-success value))))))

(defn read
  "read a keyword value from AsyncStorage and invokes the
  callback upon completion with the processed value.
  Returns a promise according to the official documentation."
  [[{:keys [id on-success on-error] :or {on-success identity on-error identity}}]]
  (let [path  (normalize id)]
    (.getItem fl/async-storage path
              (fn [error result]
                (if error (on-error error)
                  (on-success (read-string result)))))))

(defn remove!
  "takes a keyword an a value (clojure) and writes that value to
  AsyncStorage from ReactNative. Returns a promise according to the
  official documentation"
  [[{:keys [id on-success on-error] :or {on-success identity on-error identity}}]]
  (let [path  (normalize id)]
    (.removeItem fl/async-storage path
                 (fn [error]
                   (if error (on-error error)
                     (on-success))))))