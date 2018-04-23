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

;; based on the nice quote blog post
;; https://8thlight.com/blog/colin-jones/2012/05/22/quoting-without-confusion.html
(defmacro delay-js
  "same as Clojurescript delay but supports equivalence comparison based on body"
  [& body]
  `(new hive.rework.core/DelayJS '~@(rest &form) ;; use arguments as data
                                 (fn [] ~@body)
                                 nil))
