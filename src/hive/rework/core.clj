(ns hive.rework.core
  (:require [clojure.walk :as walk]
            [cljs.spec.alpha :as s]))

(defmacro <?
  "Like <! but throws errors."
  [port]
  `(let [value# (cljs.core.async/<! ~port)]
     (if (instance? js/Error value#)
       (throw value#)
       value#)))

(defmacro go-try
  "Same as (go (try ...)). No catch clause is introduced !"
  [& body]
  `(cljs.core.async/go
     (try ~@body)))

;; based on the nice quote blog post
;; https://8thlight.com/blog/colin-jones/2012/05/22/quoting-without-confusion.html
;(defmacro delay
;  "same as Clojurescript delay but supports equivalence comparison based
;   the arguments. Useful for avoiding executing effects inside function
;
;   The two arity form takes a promise and a transducer. The promise is
;   transformed into a channel and the transducer is used for its values"
;  ([effect]
;   `(new hive.rework.core/DelayEffect
;         '~@(rest &form) ;; use arguments as data
;         (fn [] ~effect)
;         nil))
;  ([promise xform]
;   `(new hive.rework.core/DelayEffect
;         '~@(rest &form) ;; use arguments as data
;         (fn [] (hive.rework.util/channel ~promise ~xform))
;         nil)))

(defmacro env
  "retrieve the environment variables at COMPILE TIME. Returns
  a map of env vars as keywords"
  []
  (walk/keywordize-keys (into {} (System/getenv))))

