(ns hive.rework.util
  (:require [cljs.core.async :as async :refer [go]]
            [clojure.spec.alpha :as s]
            [cljs.pprint :as print]))

(defn chan? [x] (satisfies? cljs.core.async.impl.protocols/Channel x))

(defn channel
  "transforms a promise into a channel. Catches js/Errors and puts them in the
  channel as well. If the catch value is not an error, yields an ex-info with
  ::promise-rejected as cause otherwise yields the js/Error provided"
  ([promise]
   (channel promise ::promise-rejected))
  ([promise cause]
   (let [result (async/chan 1)]
     (-> promise
         (.then #(async/put! result %))
         (.catch #(if (instance? js/Error %)
                    (async/put! result %)
                    (async/put! result (ex-info "oops" % cause)))))
     result)))

;; HACK: https://stackoverflow.com/questions/27746304/how-do-i-tell-if-an-object-is-a-promise
(defn promise?
  [value]
  (exists? (.-then value)))

(defn- print-warning!
  [e pipe request]
  (if (instance? js/Error (ex-data e))
    (.warn js/console (ex-data e))
    (do (.info js/console (clj->js pipe) (pr-str request))
        (.warn js/console (ex-message e) (str (ex-cause e))))))

(defn with-ns
  "modify a map keys to be namespaced with ns"
  [ns m]
  (zipmap (map #(keyword ns %) (keys m))
          (vals m)))

(defn keywordize
  "transforms a js object into a clojure version with keywords as keys"
  [o]
  (js->clj o :keywordize-keys true))

(defn log!
  "pretty prints the input and returns it"
  [o]
  (do (.log js/console o)
      o))

(defn validate
  "validates the request against the provided spec. Returns the request if valid
  or an ex-info with cause otherwise"
  ([spec value cause]
   (if (s/valid? spec value) value
     (ex-info (s/explain-str spec value)
              (s/explain-data spec value)
              cause)))
  ([spec cause]
   (fn validate* [value] (validate spec value cause))))

(defn error? [o] (instance? js/Error o))

(def bypass-error (halt-when error?))


