(ns hive.services.location
  (:require [hive.rework.core :as work]
            [hive.services.raw.location :as location]
            [hive.rework.util :as tool]
            [hive.queries :as queries]))

(defn tx-position
  [data]
  [{:user/id (:user/id data)
    :user/position (dissoc data :user/id)}])

(def update-position (comp tx-position
                           (work/inject :user/id queries/user-id)
                           location/point
                           tool/keywordize))

(def set-location! (comp work/transact! update-position))

(def defaults {::location/enableHighAccuracy true
               ::location/timeInterval       3000
               ::location/callback           set-location!})

;; TODO: write a function that will retry every now and then to set up a location
;; listener if the previous set didnt succeed
