(ns hive.rework.util
  (:require [cljs.core.async :as async :refer [go]]
            [clojure.spec.alpha :as s]
            [cljs.pprint :as print]))

(defn chan? [x] (satisfies? cljs.core.async.impl.protocols/Channel x))

(defprotocol Pipe*
  (unfold [this] "unfold (disassemble) this pipe into its constituents parts recursively"))

(defn pipe?
  "a pipe should behave just like a callable collection; with the exception of
  implementing the Pipe* marker protocol"
  [x]
  (and (satisfies? cljs.core/IFn x)
       (satisfies? cljs.core/ICollection x)
       (satisfies? Pipe* x)))

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
  [e]
  (let [error (if (instance? js/Error (ex-data e)) (ex-data e) e)]
    (.warn js/console error)))

;; TODO: only print stacktrace if we are in DEBUG mode
;; TODO: allow returning pipes to have dynamic pipe dispatch?
(defrecord Pipe [sections]
  cljs.core/IFn
  (-invoke [this request]
    (go
      (loop [queue  (unfold this)
             value  request]
        (if (instance? js/Error value)
          (do (print-warning! value) value) ;; short-circuit
          (let [f   (first queue)
                rr  (f value)
                rr2 (if (promise? rr) (channel value) rr)
                ;; get the value sync or async
                rr3 (if (chan? rr) (async/<! rr2) rr2)]
            (if (empty? (rest queue)) rr3
              (recur (rest queue) rr3)))))))
  Pipe*
  (unfold [this]
    (flatten ;; unroll the individual pipes into a bigger one
      (for [p (:sections this)]
        (if-not (pipe? p) p
          (unfold p))))))

(defn with-ns
  "modify a map keys to be namespaced with ns"
  [ns m]
  (zipmap (map #(keyword ns %) (keys m))
          (vals m)))

(defn keywordize
  "transforms a js object into a clojure version with keywords as keys"
  [o]
  (js->clj o :keywordize-keys true))

(defn log
  "pretty prints the input and returns it"
  [o]
  (do (print/pprint o)
      o))

(defn validate
  "validates the request against the provided spec. Returns the request if valid
  or an ex-info with cause otherwise"
  [spec value cause]
  (if (s/valid? spec value) value
    (ex-info (s/explain-str spec value)
             (s/explain-data spec value)
             cause)))
