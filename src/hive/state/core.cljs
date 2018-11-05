(ns hive.state.core
  (:require [hive.utils.miscelaneous :as tool]
            [datascript.core :as data]
            [hive.state.queries :as queries]))

(def tokens (tool/with-ns "ENV"
              (tool/keywordize
                (js/require "./resources/config.json"))))

;;FIXME: this should come from the server, not being hardcoded
(def cities (js->clj (js/require "./resources/cities.json")
                     :keywordize-keys true))

;; Storage types
; - :store/entity -> stores every datom for this entity in sqlite local database
; - :store/secure -> stores this datom in a secure store locally ... bypasses sqlite
; - :store/sync   -> stores every datom for this entity in sqlite and remotely

(def schema {:user/uid              {:db.unique  :db.unique/identity
                                     :store.type :store/entity}
             :user/password         {:store.type :store/secure}
             :user/city             {:db.valueType   :db.type/ref
                                     :db.cardinality :db.cardinality/one}
             :user/goal             {:db.valueType   :db.type/ref
                                     :db.cardinality :db.cardinality/one}
             :user/directions       {:db.valueType   :db.type/ref
                                     :db.cardinality :db.cardinality/one}
             ;; server support data
             :city/name             {:db.unique :db.unique/identity
                                     :store.type :store/entity}
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
             ;; GTFS entities
             :route/id              {:db.unique :db.unique/identity
                                     :store.type :store/entity}
             :trip/id               {:db.unique :db.unique/identity
                                     :store.type :store/entity}
             :stop/id               {:db.unique :db.unique/identity
                                     :store.type :store/entity}
             :trip/route            {:db.valueType :db.type/ref}
             :trip/service          {:db.valueType :db.type/ref}
             :stop_times/trip       {:db.valueType :db.type/ref}
             :stop_times/stop       {:db.valueType :db.type/ref}})

;; needs to be an array of maps. This will be used for data/transact!
(defn init-data
  [db]
  (let [uid    (data/q queries/user-id db)
        result (map #(tool/with-ns "city" %) cities)]
    (when (nil? uid) ;; we dont have a user - create a placeholder for it
      (concat result [{:user/uid  ""  ;; dummy
                       :user/city [:city/name "Frankfurt am Main"]}]))))
