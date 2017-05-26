(ns hive.wrappers.storage
  "wrapper around local storage database
  https://facebook.github.io/react-native/docs/asyncstorage.html"
  (:require [clojure.string :as str]
            [hive.foreigns :as fl]))

(defn write!
  "takes a keyword an a value (clojure) and writes that value to
  AsyncStorage from ReactNative. Returns a promise according to the
  official documentation"
  [[key value]]
  (let [k-ns   (str/split (namespace key) #"\." "/")
        k-name (name key)
        path  (str/join "/" (conj k-ns k-name))]
    (.setItem fl/async-storage path (str value))))

(defn read
  "read a keyword value from AsyncStorage and invokes the
   callback upon completion with the processed value.
  Returns a promise according to the official documentation."
  [[key callback]]
  (let [k-ns   (str/split (namespace key) #"\." "/")
        k-name (name key)
        path  (str/join "/" (conj k-ns k-name))]
    (if callback
      (.getItem fl/async-storage path #(callback (cljs.reader/read-string %2)))
      (.getItem fl/async-storage path))))