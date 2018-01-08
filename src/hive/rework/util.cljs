(ns hive.rework.util
  (:require [cljs.core.async :refer-macros [go go-loop]]
            [cljs.core.async :as async]))

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

;; TODO: allow returning pipes to have dynamic pipe dispatch
(defrecord Pipe [sections]
  cljs.core/IFn
  (-invoke [this request]
    (go
      (loop [queue  (unfold this)
             value  request]
        (if (instance? js/Error value) value ;; short-circuit
          (let [f   (first queue)
                rr  (f value)
                rr2 (if (chan? rr) (async/<! rr) rr)] ;; get the value sync or async
            (if (empty? (rest queue)) rr2
              (recur (rest queue) rr2)))))))
  cljs.core/ICollection
  (-conj [coll o] (update coll :sections conj o))
  Pipe*
  (unfold [this]
    (flatten ;; unroll the individual pipes into a bigger one
      (for [p (:sections this)]
        (if-not (pipe? p) p
          (unfold p))))))
