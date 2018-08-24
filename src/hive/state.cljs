(ns hive.state
  (:require [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [cljs.spec.alpha :as s]
            [expound.alpha :as expound]))

(s/def ::token (s/and string? not-empty))
(s/def ::MAPBOX ::token)
(s/def ::FIREBASE_API_KEY ::token)
(s/def ::FIREBASE_AUTH_DOMAIN ::token)
(s/def ::FIREBASE_DATABASE_URL ::token)
(s/def ::FIREBASE_STORAGE_BUCKET ::token)

(s/def ::env (s/keys :req-un [::MAPBOX
                              ::FIREBASE_API_KEY
                              ::FIREBASE_AUTH_DOMAIN
                              ::FIREBASE_DATABASE_URL
                              ::FIREBASE_STORAGE_BUCKET]))

;; stop compilation if the required env vars are not provided
(defn- fetch-env
  "takes a s/keys spec and returns m with only the unqualified keys
   specified in spec. Throws an Error if m does not conform to spec"
  [env]
  (let [data (apply hash-map (rest (s/form ::env)))
        ks   (map #(keyword (name %))
                   (concat (:req-un data) (:opt-un data)))
        m    (select-keys env ks)]
    (if (s/valid? ::env m) m
      (js/console.error (expound/expound-str ::env m)))))

(def tokens (tool/with-ns "ENV" (fetch-env (work/env))))

;;FIXME: this should come from the server, not being hardcoded
(def cities (js->clj (js/require "./assets/cities.json")
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
(def init-data
  (concat (map #(tool/with-ns "city" %) cities)
          [{:user/uid  ""
            :user/city [:city/name "Frankfurt am Main"]}])) ;; dummy
