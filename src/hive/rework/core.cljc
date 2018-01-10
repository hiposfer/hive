(ns hive.rework.core
  "mini framework for handling state in a reagent app.

  By using transact! all effectful functions can be made
  pure, thus easing testing significantly.

  Furthermore since they are independent of the context of their
  invocation, generative testing is even possible"
  (:require [datascript.core :as data]
            [hive.rework.tx :as rtx]
            [reagent.core :as r]
            [hive.rework.util :as tool]))

;; Before creating this mini-framework I tried re-frame and
;; Om.Next and I decided not to use either

;; re-frame has the problem that it uses an internal registry for
;; its event handlers. Since only a keyword is used to represent it
;; the user is left blind with respect to "what does this subscription returns?"
;; Furthermore since it is based on an eventful model, changing a little thing
;; in the state can lead to a cascade of changes, leaving the user wondering
;; "how did my app get into this state". There are plugins and devtools to figure
;; that out, but in my opinion fighting against the system is never a good idea.

;; Om.Next is a beast on its own. I think that the way that things are envision
;; is the right way to go for a long and big project. However Om.Next is too raw!!
;; It definitely doesnt feel natural to work with it. The user needs to define
;; a UI through `defui` but this itself is not the UI; you still need to use
;; om/factory to actually create something that you can use. Furthermore the
;; reconciler is a pain in the ass to work with. There is a million ways to
;; create, query and transact on your app state. Having to decide all that
;; up front is just too much!

;; Having said that, both frameworks have their strengths. Om.Next promotes
;; the "render a tree but think in graphs (app state)" mentality. I think that is
;; better than re-frame simple Clojure map. re-frame on the other hand uses
;; reagent to achieve automatic updates and hiccup ui declaration. That is
;; also great and it feels much more natural to work with. Also promoting
;; pure functions by returning data both in the subcriptions and in the
;; mutations is great.

;; rework tries to combine the strengths of both while mitigating its weaknesses
;; with an extra goal of trying not to reinvent the wheel. Therefore a couple
;; of decision are made up front for the user.
;; - the app state MUST be represented with a Datascript connection. This way
;; we can think in graph while re-using the code from Datascript. Pulling as in
;; Om.Next comes for free.
;; - the app must use reagent. As described in reagent.tx, we dont want to recreate
;; all the functionality that they have so reagent will server us as rendering model
;; - the app should use Clojure's core.async to handle all asynchronous operations
;; - the app state can only be "changed" through the use of pure functions

;; the way to combine functionality in REWORK is with "pipes". See below for a
;; full description

;; Holds the current state of the complete app
(defonce ^:private conn (data/create-conn))

(defn init!
  ([schema init-data]
   (let [result (data/create-conn schema)]
     (data/transact! result init-data) ;; populates the DataScript in-memory database
     (rtx/listen! result)
     (set! conn result)))
  ([schema]
   (set! conn (data/conn-from-datoms (data/datoms @conn :eavt) schema))
   (rtx/listen! conn)))

(defn pull
  "same as datascript pull but uses the app state as connection"
  [selector eid]
  (data/pull @conn selector eid))

(defn pull!
  "same as datascript/pull but returns a ratom which will be updated
  every time that the value of conn changes"
  [selector eid]
  (r/track rtx/pull* (::rtx/ratom @conn) selector eid))

(defn entity
  "same as datascript/entity but uses the app state as connection"
  [eid]
  (data/entity @conn eid))

(defn entity!
  "same as datascript/entity but returns a ratom which will be updated
  every time that the value of conn changes"
  [eid]
  (r/track rtx/entity* (::rtx/ratom @conn) eid))

(defn q
  "same as datascript/q but uses the app state as connection"
  [query & inputs]
  (apply data/q query @conn inputs))

(defn q!
  "Returns a reagent/atom with the result of the query.
  The value of the ratom will be automatically updated whenever
  a change is detected"
  [query & inputs]
  (r/track rtx/q* query (::rtx/ratom @conn) inputs))

(defn transact!
  "'Updates' the DataScript state with tx-data.

   See Datomic's API documentation for more information.
   http://docs.datomic.com/transactions.html

   Returns tx-data"
  [tx-data]
  (do (data/transact! conn tx-data)
      tx-data))

(defn inject
  "runs query with provided inputs and associates its result into m
  under key"
  [m key query & inputs]
  (let [result (apply q query inputs)]
    (assoc m key result)))

;; Pipes are a combination of 3 concepts:
;; - UNIX pipes
;; - Haskell's Maybe Monad
;; - Haskell's IO Monads
;; Pipes are a sequence of functions executed one after the other with each
;; function receiving the result of the previous one
;; Pipes have 3 main purposes:
;; - allow the combination of sync and async code into a single go loop
;; - separate effectful code from pure functions
;; - provide a way to convey errors during the process without silently
;;   throwing on the background
;; The problem with normal effectful code is that is it is not possible to
;; use it without the user-function becoming effectful itself. This leads to
;; a propagation of impure-functions for every action that the user wants to
;; perform. With pipes, it is possible to separate the effectful functions from
;; the pure ones. Furthermore since the input to each function is synchronous
;; there is no need for glue code in synchronous functions.
;; Finally, there should be a way to stop a pipe execution in case something goes
;; wrong. Therefore, pipes are short-circuited if a js/Error is returned by any
;; function. In that case, the js/Error is returned as the result of the pipe i.e.
;; it is NOT thrown.


(defn pipe
  "Takes a set of functions and returns a fn that is the composition of those fns.
  The returned fn takes a single argument (request), applies the leftmost of fns to
  it, the next fn (left-to-right) to the result, etc (like transducer composition).

  Returns a channel which will receive the result of the body when completed

  If any function returns an exception, the execution will stop and returns it

  Both sync and async functions are accepted"
  [f g & more]
  (tool/->Pipe (concat [f g] more)))

(defn- throw-err
  [e]
  (if (instance? js/Error e) (throw e)
    e))

#?(:clj
   (defmacro <?
     "Like <! but throws errors."
     [ch]
     `(throw-err (cljs.core.async/<! ~ch))))

;(data/transact! (:conn @app) [{:user/city [:city/name "Frankfurt am Main"]}])

;(q '[:find [(pull ?entity [*]) ...]
;     :where [?entity :city/name ?name]))

;(q queries/cities)

;conn
