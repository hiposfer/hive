(ns hive.rework.core
  "mini framework for handling state in a reagent app.

  By using transact! all effectful functions can be made
  pure, thus easing testing significantly.

  Furthermore since they are independent of the context of their
  invocation, generative testing is even possible"
  (:require [datascript.query :as datascript]
            [posh.plugin-base :as pbase]
            [posh.reagent :as posh]
            [datascript.core :as data]
            [hive.queries :as queries]))

;; TODO: there is still a piece missing in this mini-framework
;; the ability to pass messages to channels !!
;; it could be possible by having a simple
;; `send :chann-name` function which takes a
;; channel name as declared by system and passes it
;; to the given function

;; Holds the current state of the complete app
(defonce ^:private app (atom nil))

(defn init! [value] (when (nil? @app) (reset! app value)))

(defn query
  "Returns the data stored in the app state according to query"
  ([query]
   (datascript/q query @(:conn @app)))
  ([query params]
   (datascript/q query @(:conn @app) params)))

(defn query!
  "Returns a reagent/atom whose value will be updated anytime that
  its app state values are updated"
  ([query]
   (pbase/q posh/dcfg query (:conn @app)))
  ([query params]
   (pbase/q posh/dcfg query (:conn @app) params)))

(defn- inquire
  [inquiry]
  (if (vector? inquiry)
    (query inquiry)
    (query (:query inquiry) (:params inquiry))))

(defn transact!
  "'Updates' the DataScript state, where f is a function that will take
   the result of the inquiry and any supplied args and return tx-data
   to use with DataScript transact!

   inquiry can either be a query vector like [:find ?foo :where [_ :bar ?foo]]
   or a map with {:query [datascript-query]
                  :params [parameters to use :in query]}"
  ([tx-data]
   (data/transact! (:conn @app) tx-data))
  ([inquiry f]
   (let [result (inquire inquiry)]
     (data/transact! (:conn @app) (f result))))
  ([inquiry f x]
   (let [result (inquire inquiry)]
     (data/transact! (:conn @app) (f result x))))
  ([inquiry f x & more]
   (let [result (inquire inquiry)]
     (data/transact! (:conn @app) (apply f result x more)))))


;(data/pull @conn [:user/name :user/age] [:user/id "1"])

;(data/q '[:find ?name ?geometry ?bbox
;          :where [?entity :name]
;                 [?entity :bbox ?bbox]
;                 [?entity :name ?name]
;                 [?entity :geometry ?geometry]]
;        @(:conn @app))

;(query '[:find ?entity .
;         :in $ ?name
;         :where [?entity :name ?name]
;                [?entity :bbox]
;                [?entity :geometry]]
;       "Frankfurt am Main")

;(data/entity @(:conn @app) 1)

;(:conn @app)

;(data/transact! (:conn @app) [{:user/city [:city/name "Frankfurt am Main"]}])

;(query '[:find ?name ?bbox ?geometry
;         :where [_ :user/city ?city]
;                [?city :city/name ?name]
;                [?city :bbox ?bbox]
;                [?city :geometry ?geometry]])

;(transact! queries/user-id
;  (fn [result city-name]
;    [{:user/id result
;      :user/city [:city/name city-name]}])
;  "Offenburg")

;(query queries/cities)
;(query)