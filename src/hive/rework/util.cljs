(ns hive.rework.util
  (:require [cljs.core.async :refer-macros [go-loop]]
            [cljs.core.async :as async]
            [hive.rework.core :as rework]))


(defn chan? [x] (satisfies? cljs.core.async.impl.protocols/Channel x))

(defn inject
  "runs query with provided inputs and associates its result into m
  under key"
  [m key query & inputs]
  (let [result (apply rework/q query inputs)]
    (assoc m key result)))

comp

(defn pipe
  "Takes a set of functions and returns a fn that is the composition of those fns.
   The returned fn takes a variable number of args, applies the leftmost of fns to
   the args, the next fn (left-to-right) to the result, etc (like transducer composition).

   Returns a channel which will receive the result of the body when completed

   If any function returns an exception, the execution will stop and returns it

   Both sync and async functions are accepted"
  [f g & more]
  (fn [& args]
    (go
      (loop [stack   (concat [f g] more)
             result  args]
        (if (instance? js/Error result) result
          (let [ff     (first stack)
                rr     (apply ff result)]
            (if (empty? (rest stack))
              (if-not (chan? rr) rr
                (async/<! rr))
              (if (chan? rr)
                (recur (rest stack) (async/<! rr))
                (recur (rest stack) rr)))))))))
