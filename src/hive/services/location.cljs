(ns hive.services.location
  (:require [hive.rework.core :as work]
            [hive.services.raw.location :as location]))

(def set-location (comp work/transact! location/update-position))

(def defaults {::location/enableHighAccuracy true
               ::location/timeInterval 3000
               ::location/callback set-location})

;; TODO: write a function that will retry every now and then to set up a location
;; listener if the previous set didnt succeed