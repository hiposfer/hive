(ns hive.rework.core
  "mini framework for handling state in a reagent app.

  By using transact! all effectful functions can be made
  pure, thus easing testing significantly.

  Furthermore since they are independent of the context of their
  invocation, generative testing is even possible"
  (:require [datascript.core :as data]
            [hive.rework.tx :as rtx]
            [reagent.core :as r]))

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
;; the render tree but think in graph (app state) mentality. I think that is
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
;; - the app must use Clojure's core.async in the form of permanent services; this
;; way we can represent inherently asynchronous operations as sequential
;; - the app state can only be "changed" through the use of pure functions

;; Holds the current state of the complete app
(defonce ^:private conn (data/create-conn))

(defn reset!
  ([schema init-data]
   (rtx/unlisten! conn)
   (let [result (data/create-conn schema)]
     (data/transact! result init-data) ;; populates the DataScript in-memory database
     (rtx/listen! result)
     (set! conn result)))
  ([schema]
   (rtx/unlisten! conn)
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
  (r/track rtx/pull* (::ratom @conn) selector eid))

(defn entity
  "same as datascript/entity but uses the app state as connection"
  [eid]
  (data/entity @conn eid))

(defn entity!
  "same as datascript/entity but returns a ratom which will be updated
  every time that the value of conn changes"
  [eid]
  (r/track rtx/entity* (::ratom @conn) eid))

(defn q
  "same as datascript/q but uses the app state as connection"
  [query & inputs]
  (apply data/q query @conn inputs))

(defn q!
  "Returns a reagent/atom with the result of the query.
  The value of the ratom will be automatically updated whenever
  a change is detected"
  [query & inputs]
  (r/track rtx/q* query (::ratom @conn) inputs))

(defn- inquire
  [inquiry]
  (if (vector? inquiry)
    (q inquiry)
    (q (:query inquiry) (:args inquiry))))

(defn transact!
  "'Updates' the DataScript state, where f is a function that will take
   the result of the inquiry and any supplied args and return tx-data
   to use with DataScript transact!

   inquiry can either be a query vector like [:find ?foo :where [_ :bar ?foo]]
   or a map with {:query [datascript-query]
                  :args  [parameters to use :in query]}
   Returns the result of the result of the query with the new state"
  ([tx-data]
   (data/transact! conn tx-data)
   @conn)
  ([inquiry f & args]
   (let [result (inquire inquiry)]
     (data/transact! conn (apply f result args))
     (inquire inquiry))))

;(data/transact! (:conn @app) [{:user/city [:city/name "Frankfurt am Main"]}])

;(:conn (:state @app))

;(q '[:find (pull ?city [*]) .
;     :where [_ :user/city ?city]])
;
;(q '[:find [(pull ?entity [*]) ...]
;     :where [?entity :city/name ?name]))
