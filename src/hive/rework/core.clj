(ns hive.rework.core)

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

(defmacro delay-js
  "same as Clojurescript delay but supports equivalence comparison based on body"
  [& body]
  `(new hive.rework.core/DelayJS '~@(rest &form) ;; use arguments as data
                                 (fn [] ~@body)
                                 nil))
