(ns hive.state.schema
  (:require [hiposfer.gtfs.edn :as gtfs]))

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

(def schema (merge-with into
              ;; GTFS entities
              (gtfs-schema)
              ;; hive schema
              {:user/uid              {:db/unique    :db.unique/identity
                                       :hive.storage :sqlite/store}
               :user/password         {:hive.storage :sqlite/ignore}
               :user/area             {:db/valueType   :db.type/ref
                                       :db/cardinality :db.cardinality/one}
               :user/goal             {:db/valueType   :db.type/ref
                                       :db/cardinality :db.cardinality/one}
               :user/directions       {:db/valueType   :db.type/ref
                                       :db/cardinality :db.cardinality/one}
               ;; server support data
               :area/id               {:db/unique    :db.unique/identity
                                       :hive.storage :sqlite/store}
               ;; mapbox data
               :place/id              {:db/unique :db.unique/identity}
               ;; ephemeral data
               :session/uuid          {:db/unique :db.unique/identity}
               ;; server response data
               :directions/uuid       {:db/unique :db.unique/identity}
               :directions/steps      {:db/valueType   :db.type/ref
                                       :db/cardinality :db.cardinality/many
                                       :db/isComponent true}
               :step/maneuver         {:db/valueType   :db.type/ref
                                       :db/cardinality :db.cardinality/one}
               :step/trip             {:db/valueType   :db.type/ref
                                       :db/cardinality :db.cardinality/one}
               ;; needed to tell datascript to keep only 1 of these
               :react.navigation/name {:db/unique :db.unique/identity}
               ;; provide a way for ERRORS to be registered
               :error/id              {:db/unique :db.unique/identity}}))
