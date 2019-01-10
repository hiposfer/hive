(ns hive.state.core
  "This namespace contains a mini framework which is just basically glue code
   between the different libraries used. Its main purpose is state management
   for Clojurescript Single Page Applications. Its secondary purposes are
   ease of use, no magic involved and testing support. Read below for more
   information on these

  This mini-framework simply uses the technologies out there to achieve its
  purpose, thus avoiding reinventing the wheel. A couple of decisions are made
  up front for the user.
  - the app state MUST be represented with a single Datascript connection. This
    way it is possible to represent the app state as a complex interconnected
    structure while being able to navigate it through its query/pull system
  - the app must use reagent. As described in reagent.tx, we dont want to recreate
    all the functionality that they have so reagent will serve us as rendering model
    and as reactive data framework

  Since the app is represented as a single Datascript atom. All state changes
  can be represented by Datascript transaction, which means that the functions
  that create those transactions can be pure, which will later on have an impact
  on testing.
  ;; example
  (state/transact [{:user/id :user/favorites data}]
  (state/transact (compute-favorites data))
  (state/trasact (async-chan-with-transaction))

  However no app can survive only with state changes. You always need to perform
  server synchronization, api calls, read/write from local storage, among others.
  Those *intended-effects* (as opposed to side-effects) are inevitable. This
  framework doesnt try to avoid them nor to mark them as evil, instead we understand
  that they are necessary. We encourage the logic of side effect to be encapsulated
  in frameworks intended for it, like Promises. However we prefer for functions to
  *declare* their intended-effects instead of performing them.
  This is meant to ease testing. It is much more easy to test a function that returns
  a sequence of vector with values inside than it is to test a function which returns
  nothing but performs some effects inside.
  ;; example
  (state/transact [http/json url options])
  (state/transact (delay (hide-keyboard))

  Last but not least is error handling. We propose the inclusion of errors as
  data values as opposed to their classical view of 'anomalous or exceptional
  conditions requiring special processing – often changing the normal flow of
  program execution'. Therefore we separate between expected errors like, no
  internet connection, malformed json response or unauthorized app permissions
  from those arising from incorrect code a.k.a bugs"
  (:require [hive.utils.miscelaneous :as tool]
            [datascript.core :as data]
            [hiposfer.rata.core :as rata]
            [clojure.walk :as walk]
            [hiposfer.gtfs.edn :as gtfs]))

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
;; with an extra goal of trying not to reinvent the wheel

(def gtfs-data (js->clj (js/require "./resources/gtfs.json")
                        :keywordize-keys true))

;; General Transfer Feed Specification - entities
;; identities
(defn- gtfs-schema
  []
  (let [identifiers (gtfs/identifiers gtfs-data)]
    (into (sorted-map) (remove nil?)
          (for [field (gtfs/fields gtfs-data)]
            (cond
              (:unique field)
              [(field :keyword) {:db.unique :db.unique/identity}]

              (gtfs/reference? identifiers field)
              [(field :keyword) {:db/type :db.type/ref}])))))

(def tokens (tool/with-ns "ENV"
              (tool/keywordize
                (js/require "./resources/config.json"))))

(def schema (merge-with into
              ;; GTFS entities
              (gtfs-schema)
              ;; hive schema
              {:user/uid              {:db.unique    :db.unique/identity
                                       :hive.storage :sqlite/store}
               :user/password         {:hive.storage :sqlite/ignore}
               :user/area             {:db.valueType   :db.type/ref
                                       :db.cardinality :db.cardinality/one}
               :user/goal             {:db.valueType   :db.type/ref
                                       :db.cardinality :db.cardinality/one}
               :user/directions       {:db.valueType   :db.type/ref
                                       :db.cardinality :db.cardinality/one}
               ;; server support data
               :area/id               {:db.unique    :db.unique/identity
                                       :hive.storage :sqlite/store}
               ;; mapbox data
               :place/id              {:db.unique :db.unique/identity}
               ;; ephemeral data
               :session/uuid          {:db.unique :db.unique/identity}
               ;; server response data
               :directions/uuid       {:db.unique :db.unique/identity}
               :directions/steps      {:db.valueType   :db.type/ref
                                       :db.cardinality :db.cardinality/many}
               :step/maneuver         {:db.valueType   :db.type/ref
                                       :db.cardinality :db.cardinality/one}
               :step/trip             {:db.valueType   :db.type/ref
                                       :db.cardinality :db.cardinality/one}
               ;; needed to tell datascript to keep only 1 of these
               :react.navigation/name {:db.unique :db.unique/identity}
               ;; provide a way for ERRORS to be registered
               :error/id              {:db.unique :db.unique/identity}}))

(defonce conn (rata/listen! (data/create-conn schema)))

(defn pull!
  "same as datascript/pull but returns a ratom which will be updated
  every time that the value of conn changes"
  [selector eid]
  (rata/pull! conn selector eid))

(defn q!
  "Returns a reagent/atom with the result of the query.
  The value of the ratom will be automatically updated whenever
  a change is detected"
  [query & inputs]
  (rata/q! query conn inputs))

;; utility to not be so verbose
(defn db [] (deref conn))

(declare transact!)

(defn- demunge-effect
  [[f & args]]
  (into [(.-name f)]
        (walk/postwalk (fn [v]
                         (cond
                           (data/db? v)
                           "#DB{...}"

                           (and (vector? v) (fn? (first v)))
                           (into [(.-name (first v))] (rest v))

                           :else v))
                       args)))

(defn- log-object
  [value]
  (cond
    ;; JS promise - wait for its value then transact it
    (tool/promise? value)
    (let [id (data/squuid)]
      (.. value (then (fn [result]
                        (js/console.log #js {:type "promise" :status "resolved" :id (pr-str id)
                                             :value (clj->js (log-object result))})
                        (identity result)))
                (catch (fn [error] (js/console.warn #js {:type "promise" :id (pr-str id) :status "rejected"
                                                         :error error}))))
      {:type "promise" :status "pending" :id (pr-str id)})

    ;; functional side effect declaration
    ;; Execute it and try to execute its result
    (and (vector? value) (fn? (first value)))
    {:type "effect" :value (pr-str (demunge-effect value))}

    ;; side effect declaration wrapped with delay to allow testing
    ;; Force it and try to execute its result
    (delay? value)
    {:type "native effect" :value (pr-str value)}

    ;; transaction item
    (or (vector? value) (data/datom? value) (map? value))
    {:type "tx" :value (pr-str value)}

    ;; datascript transaction
    (seq? value) {:type "transaction" :value (pr-str value)}))

(defn- execute!
  [transaction value]
  ;; do nothing in production
  (when (true? js/__DEV__)
    (js/console.log (clj->js (log-object value))))

  (cond
    ;; JS promise - wait for its value then transact it
    (tool/promise? value)
    (. value (then transact!))

    ;; functional side effect declaration
    ;; Execute it and try to execute its result
    (and (vector? value) (fn? (first value)))
    (do (execute! [] (apply (first value) (rest value)))
        (identity transaction))

    ;; side effect declaration wrapped with delay to allow testing
    ;; Force it and try to execute its result
    (delay? value)
    (do (execute! [] (deref value))
        (identity transaction))

    ;; simple datascript transaction
    (or (map? value) (vector? value) (data/datom? value))
    (conj transaction value)
    ;; otherwise just keep reducing
    :else transaction))

(defn transact!
  "Single entry point for 'updating' the app state. The behaviour of
  transact! depends on the arguments.

   data can be:
   - a standard Datascript transaction. See Datomic's API documentation for more
    information. http://docs.datomic.com/transactions.html
   - a channel yielding one or more transactions
   - a vector whose first element is a function and the rest are its argument.
     The function will be executed and its return value is used in-place of
     the original one. Useful for keeping functions side-effect free
   - a delay whose content will be forced. Its return value is used in-place
     of the original one. Useful for side-effect free Js interop
   - a Js Promise yielding a single transaction. Useful for doing
     asynchronous Js calls without converting them to a async channel

   Not supported data types are ignored"
  ([data]
   (transact! data nil))
  ([data tx-meta]
   (when (sequential? data)
     (let [tx (reduce execute! [] data)]
       (data/transact! conn tx tx-meta)))))
