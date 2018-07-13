(ns hive.services.store
  (:require [cljs.reader :refer [read-string]]
            [hive.rework.util :as tool]
            [hive.foreigns :as fl]))

;https://docs.expo.io/versions/latest/sdk/securestore.html

;Expo.SecureStore.setItemAsync(key, value, options)
(defn save!
  "Takes a map of namespaced keywords and stores it. Returns a promise
   containing a map of the stored values

  Example:
  (save! {:hive/foo 1 :hive/bar 2} options})

  ::options can be any of the options defined by Expo. See
  https://docs.expo.io/versions/latest/sdk/securestore.html"
  ^js/Promise
  [request options]
  (let [opts    (clj->js (or options {}))
        proms   (for [[k v] request]
                  (.. fl/Expo
                      -SecureStore
                      (setItemAsync (munge k) (pr-str v) opts)
                      (then #(vector k v))
                      (catch identity)))] ;; return error
    (.. (js/Promise.all (clj->js proms))
        (then #(into {} (remove tool/error? %))))))

;(.. (save! {} {:foo/bar 2})
;    (then println))

;Expo.SecureStore.getItemAsync(key, options)
(defn load!
  "takes a sequence of namespaced keywords and returns a promise containing a
  map of keys to values"
  ^js/Promise
  [options & ks]
  (let [opts  (clj->js options)
        proms (for [k (distinct ks)]
                (.. fl/Expo
                    -SecureStore
                    (getItemAsync (munge k) opts)
                    (then #(if (some? %) [k (read-string %)] nil))
                    (catch identity)))] ;; return error
    (.. (js/Promise.all (clj->js proms))
        (then #(into {} (remove nil? (remove tool/error? %)))))))

;(.. (load! {} :foo/bar)
;    (then println))

;Expo.SecureStore.deleteItemAsync(key, options)
;; todo: should this function return the value previously stored there?
;; that would imply a load! before :(
(defn delete!
  "takes a sequence of namespaced keywords and returns a promise with the
  keys of the elements that were deleted"
  [options & ks]
  (let [opts  (clj->js options)
        proms (for [k (distinct ks)]
                (.. fl/Expo
                    -SecureStore
                    (deleteItemAsync (munge k) opts)
                    (then #(println k))
                    (catch identity)))] ;; return error
    (.. (js/Promise.all proms)
        (then #(into [] (remove tool/error? %))))))

;(delete! {} :foo/bar)
;(then println))
