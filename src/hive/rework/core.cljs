(ns hive.rework.core
  "mini framework for handling state in a reagent app.

  By using transact! all effectful functions can be made
  pure, thus easing testing significantly.

  Furthermore since they are independent of the context of their
  invocation, generative testing is even possible"
  (:require [datascript.core :as data]))

;; Holds the current state of the complete app
(defonce ^:private app (atom nil))

(defn init! [value] (when (nil? @app) (reset! app value)))

(defn- inquire
  [inquiry]
  (if (vector? inquiry)
    (data/q inquiry @(:conn @app))
    (data/q (:query inquiry) @(:conn @app) (:params inquiry))))

(defn transact!
  "'Updates' the DataScript state, where f is a function that will take
   the result of the inquiry and any supplied args and return tx-data
   to use with DataScript transact!

   Inquiry can either be a query vector like [:find ?foo :where [_ :bar ?foo]]
   or a map with {:query [datascript-query]
                  :params [parameters to use :in query]}"
  ([inquiry f]
   (let [result (inquire inquiry)]
     (data/transact! (:conn @app) (f result))))
  ([inquiry f x]
   (let [result (inquire inquiry)]
     (data/transact! (:conn @app) (f result x))))
  ([inquiry f x & more]
   (let [result (inquire inquiry)]
     (data/transact! (:conn @app) (apply f result x more)))))


;(:conn @app)

;(data/q '[:find ?age
;          :in $ ?name
;          :where [?e :user/name ?name]
;                 [?e :user/age ?age]]
;        @conn "alice")

;(data/pull @conn [:user/name :user/age] [:user/id "1"])

;(posh/pull conn [:user/name :user/age] [:user/id "1"])

;(posh/transact! (:conn @env.main/app)
;                [{:user/id "1"
;                  :user/name "hello world"
;                  :user/age 27}])

;(data/q '[:find ?name ?geometry ?bbox
;          :where [?entity :name]
;                 [?entity :bbox ?bbox]
;                 [?entity :name ?name]
;                 [?entity :geometry ?geometry]]
;        @(:conn @app))