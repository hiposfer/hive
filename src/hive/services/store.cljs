(ns hive.services.store
  (:require [hive.services.raw.store :as store]
            [cljs.core.async :as async]
            [cljs.spec.alpha :as s]
            [hive.rework.util :as tool]))

;https://docs.expo.io/versions/latest/sdk/securestore.html

;Expo.SecureStore.setItemAsync(key, value, options)
(defn save!
  "Takes a map of namespaced keywords and stores it. Returns a channel containing
  a map of the stored values

  Example:
  (save! {:hive/foo 1 :hive/bar 2 ::options {}})

  ::options can be any of the options defined by Expo. See
  https://docs.expo.io/versions/latest/sdk/securestore.html"
  [request]
  (let [opts    (or (::options request) {})
        request (dissoc request ::options)
        chans   (map (fn [[k v]] (store/save! k v opts)) request)
        res     (async/merge chans (count request))]
    (async/into {} res)))

;(async/take! (save! {:foo "bar" :baz "rofl"})
;             cljs.pprint/pprint
;             cljs.pprint/pprint)

;Expo.SecureStore.getItemAsync(key, options)
(defn load!
  "takes a sequence of namespaced keywords and returns a channel containing a
  map of keys to values"
  [keys]
  (let [keys  (distinct keys)
        chans (map #(store/load! % {}) keys)
        res   (async/merge chans (count keys))
        to    (async/chan (count keys) (remove tool/error?))
        clean (async/pipe res to)]
    (async/into {} clean)))

;(async/take! (load! [:foo :baz])
;             cljs.pprint/pprint
;             cljs.pprint/pprint)

;Expo.SecureStore.deleteItemAsync(key, options)
;; todo: should this function return the value previously stored there?
;; that would imply a load! before :(
(defn delete!
  "takes a sequence of namespaced keywords and returns a channel with the
  keys of the elements that were deleted"
  [keys]
  (let [keys  (distinct keys)
        chans (map #(store/delete! % {}) keys)
        res   (async/merge chans (count keys))
        to    (async/chan (count keys) (remove tool/error?))
        clean (async/pipe res to)]
    (async/into [] clean)))

;(async/take! (delete! "foo" {})
;             cljs.pprint/pprint))))

(s/fdef save!
        :args (s/cat :request (s/map-of ::store/key ::store/value))
        :ret tool/chan?)

(s/fdef load!
        :args (s/cat :ks (s/coll-of ::store/key))
        :ret tool/chan?)

(s/fdef delete!
        :args (s/cat :ks (s/coll-of ::store/key))
        :ret tool/chan?)

;(require '[clojure.spec.test.alpha :as stest])

;(stest/instrument `save!)

;(save! {:foo/bar 1})
;(load!)
