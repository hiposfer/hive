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


;; Food for thought
;; initially I envisioned rework to be more dataflow oriented, however
;; after developing it and using it on my own app I realized that it might
;; not be the best approach
;; Currently, rework works by combining the JS promise world with Cljs Async
;; and sync functions. This works wonderfully well specially if coordination
;; of multiple tasks needs to take place. Nevertheless, there are clear drawbacks:
;; - it is not possible to conditionate the execution of a part of a pipe (filter)
;;   without recurding into exceptions even if that is not what the user intended
;; - recreating functionality: since transducers are out this leaves the user to do
;;   lots of things manually which is not very user-friendly
;;
;; https://www.youtube.com/watch?v=096pIlA3GDo
;; There is a great talk by Timothy Baldbridge where he talks about several
;; Core.async patterns. The pattern followed in rework is very similar to Pedestal
;; interceptors.
;; The other patterns are also worth considering but each one presents with their own
;; problems
;; - using transducers with core.async channels
;;   - piping transducers is a very manual process
;;   - doesnt play well with promises
;;   - from my experience with core.async I would say that it generally leads the
;;       developer into very imperative language due to the way that go blocks work
;; - using interceptors ... this is actually what rework does
;; - dataflow with core.async - this is an idea that I have been wanting to explore
;;   but I have not been able to envision how to implement it properly
;;   (error handling, piping, etc)

;; what is the future of this mini-framework ?? nobody knows but it sure as hell
;; feels great to have the power of DataScript, reagent and core.async working
;; together

; this shows that exception handling is possible with core.async
; todo: is it worth investing into this idea?
;(async/go
;  (let [c (async/chan 1 (map #(throw (new js/Error %)))
;                      tool/log)]
;    (async/>! c 3)
;    (async/<! c)))



