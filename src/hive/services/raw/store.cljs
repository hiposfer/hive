(ns hive.services.raw.store
  (:require [cljs.reader :as r :refer [read-string]]
            [hive.foreigns :as fl]
            [cljs.core.async :as async]
            [cljs.spec.alpha :as s]
            [hive.rework.util :as tool]))

(s/def ::key qualified-keyword?)
(s/def ::value (s/and some? (complement fn?))) ;; is this enough?
(s/def ::options map?)

;https://docs.expo.io/versions/latest/sdk/securestore.html

;Expo.SecureStore.setItemAsync(key, value, options)
(defn save!
  "takes a namespaced key, a value and options as defined in
  https://docs.expo.io/versions/latest/sdk/securestore.html

  Returns a channel with [key value] or an Error if the value
  could not be stored"
  [key value options]
  (let [c (async/chan 1)]
    (doto ((:setItemAsync fl/Store) (munge key) (pr-str value) (clj->js options))
      (.then #(doto c (async/put! [key value])
                      (async/close!)))
      (.catch #(doto c (async/put! %)
                       (async/close!))))
    c))

;Expo.SecureStore.getItemAsync(key, options)
(defn load!
  [key options]
  (let [c     (async/chan 1)
        error (ex-info "key doesnt exists" key ::undefined-value)]
    (doto ((:getItemAsync fl/Store) (munge key) (clj->js options))
      (.then #(doto c (async/put! (if % [key (read-string %)] error))
                      (async/close!)))
      (.catch #(doto c (async/put! %)
                       (async/close!))))
    c))

;Expo.SecureStore.deleteItemAsync(key, options)
;; todo: should this function return the value previously stored there?
;; that would imply a load! before :(
(defn delete!
  [key options]
  (let [c (async/chan 1)]
    (doto ((:deleteItemAsync fl/Store) (munge key) (clj->js options))
      (.then #(doto c (async/put! key)
                      (async/close!)))
      (.catch #(doto c (async/put! %)
                       (async/close!))))
    c))

;(async/take! (delete! "foo" {})
;             cljs.pprint/pprint))))

(s/fdef save!
        :args (s/cat :k ::key :v ::value :opts ::options)
        :ret  tool/chan?)

(s/fdef load!
        :args (s/cat :k ::key :opts ::options)
        :ret  tool/chan?)

(s/fdef delete!
        :args (s/cat :k ::key :opts ::options)
        :ret  tool/chan?)