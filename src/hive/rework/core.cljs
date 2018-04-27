(ns hive.rework.core
  "mini framework for handling state in a reagent app.

  By using transact! all effectful functions can be made
  pure, thus easing testing significantly.

  Furthermore since they are independent of the context of their
  invocation, generative testing is even possible"
  (:require-macros [hive.rework.core])
  (:require [datascript.core :as data]
            [hive.rework.tx :as rtx]
            [reagent.core :as r]
            [hive.rework.util :as tool]
            [hive.rework.state :as state]
            [cljs.core.async :as async]))

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
;; all the functionality that they have so reagent will serve us as rendering model
;; - the app should use Clojure's core.async to handle all asynchronous operations
;; - the app state can only be "changed" through the use of pure functions

(defn init!
  "takes a Datascript conn and starts listening to its transactor for changes"
  [dsconn]
  (when state/conn ;; just in case
    (rtx/unlisten! state/conn)
    (rtx/unlisten! dsconn))
  (set! state/conn dsconn)
  (rtx/listen! state/conn))

;; copy of Clojure delay with equality implementation
;; useful for testing of side effects !!
(deftype DelayEffect [body ^:mutable f ^:mutable value]
  IDeref
  (-deref [_]
    (when f
      (set! value (f))
      (set! f nil))
    value)
  IPending
  (-realized? [_]
    (not f))
  IEquiv
  (-equiv [_ that]
    (when (instance? DelayEffect that)
      (= body (.-body ^DelayEffect that)))))

(defn delay-effect?
  "check if o is an instance of DelayJS"
  [o]
  (instance? DelayEffect o))

(defn pull
  "same as datascript pull but uses the app state as connection"
  [selector eid]
  (data/pull @state/conn selector eid))

(defn pull!
  "same as datascript/pull but returns a ratom which will be updated
  every time that the value of conn changes"
  [selector eid]
  (r/track rtx/pull* (::rtx/ratom @state/conn) selector eid))

(defn entity
  "same as datascript/entity but uses the app state as connection"
  [eid]
  (data/entity @state/conn eid))

(defn q
  "same as datascript/q but uses the app state as connection"
  [query & inputs]
  (apply data/q query @state/conn inputs))

(defn q!
  "Returns a reagent/atom with the result of the query.
  The value of the ratom will be automatically updated whenever
  a change is detected"
  [query & inputs]
  (r/track rtx/q* query (::rtx/ratom @state/conn) inputs))

(defn transact!
  "'Updates' the DataScript state with data.

   data can be:
   - a standard Datomic transaction. See Datomic's API documentation for more
    information. http://docs.datomic.com/transactions.html
   - a channel containing one or more transactions
   - a vector whose first element is a function and the rest are its argument.
    Useful for keeping functions side-effect free

   If an [f & args] argument is given, the function is executed and its return
   value is passed to transact! again.

   Returns Datascript transact! return value or a channel
   with the content of each transact! result. Not supported data types
   are ignored"
  ([data]
   (cond
     (tool/chan? data) ;; async transaction
     (transact! data (map identity))
     ;; functional side effect declaration
     ;; Execute it and try to transact it
     (and (vector? data) (fn? (first data)))
     (recur (apply (first data) (rest data)))
     ;; side effect declaration wrapped with DelayJS to allow testing
     ;; Force it and try to transact it
     (delay-effect? data)
     (recur (deref data))
     ;; simple transaction
     (sequential? data)
     (data/transact! state/conn data)

     :else (do (println "unknown transact! type argument" data)
               data))) ;; js/Errors, side effects with no return value ...
  ([port xform]
   (let [c (async/chan 1 (comp xform (map transact!)))]
     (async/pipe port c))))

(defn inject
  "runs query with provided inputs and associates its result into m
  under key"
  ([m key query & inputs]
   (let [result (apply q query inputs)]
     (assoc m key result)))
  ([key query] ;; not possible to have & inputs due to conflict with upper args
   (fn inject* [m] (inject m key query))))

;(data/transact! (:conn @app) [{:user/city [:city/name "Frankfurt am Main"]}])

;(q '[:find [(pull ?entity [*]) ...]
;     :where [?entity :city/name ?name]))

;(q queries/cities)

;conn
