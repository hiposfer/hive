(ns hive.state
  (:require [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [cljs.spec.alpha :as s]))

(s/def ::token (s/and string? not-empty))
(s/def ::MAPBOX ::token)
(s/def ::FIREBASE_APIKEY ::token)
(s/def ::FIREBASE_DATABASE_URL ::token)
(s/def ::FIREBASE_STORAGE_BUCKET ::token)
(s/def ::AUTH0_CLIENT_ID ::token)
(s/def ::AUTH0_DOMAIN ::token)

(s/def ::env (s/keys :req-un [::MAPBOX ::AUTH0_CLIENT_ID ::AUTH0_DOMAIN]
                     :opt-un [::FIREBASE_APIKEY ::FIREBASE_DATABASE_URL ::FIREBASE_SORAGE_BUCKET]))

;; stop compilation if the required env vars are not provided
(defn- fetch-env
  "takes a s/keys spec and returns m with only the unqualified keys
   specified in spec. Throws an Error if m does not conform to spec"
  [spec m]
  (let [data (apply hash-map (rest (s/form ::env)))
        ks   (map #(keyword (name %))
                   (concat (:req-un data) (:opt-un data)))
        m    (select-keys m ks)]
    (if (not (s/valid? spec m))
      (js/console.error "The app is misconfigured. Add env vars and rebuild."))
    m))

(def tokens (tool/with-ns "ENV" (fetch-env ::env (work/env))))

;;FIXME: this should come from the server, not being hardcoded
(def cities (js->clj (js/require "./assets/cities.json")
                     :keywordize-keys true))

(def schema {:user/city             {:db.valueType   :db.type/ref
                                     :db.cardinality :db.cardinality/one}

             :user/route            {:db.valueType   :db.type/ref
                                     :db.cardinality :db.cardinality/one}

             :user/id               {:db.unique :db.unique/identity}

             :city/name             {:db.unique :db.unique/identity}

             :session/uuid          {:db.unique :db.unique/identity}

             :route/uuid            {:db.unique :db.unique/identity}
             :route/steps           {:db.valueType   :db.type/ref
                                     :db.cardinality :db.cardinality/many}
             ;; needed to tell datascript to keep only 1 of these
             :react.navigation/name {:db.unique :db.unique/identity}
             ;; GTFS entities
             :trip/id               {:db.unique :db.unique/identity}
             :trip/route            {:db.valueType :db.type/ref}
             :trip/service          {:db.valueType :db.type/ref}
             :route/id              {:db.unique :db.unique/identity}})

;; needs to be an array of maps. This will be used for data/transact!
(def init-data
  (concat (map #(tool/with-ns "city" %) cities)
          [{:user/id -1} ;; dummy
           tokens]))


(def defaults {:user/city [:city/name "Frankfurt am Main"]})
