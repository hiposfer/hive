(ns hive.services.kamal
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str])
  (:import (goog.date DateTime)))

(defn- local-time
  "returns a compatible Java LocalDateTime string representation"
  ([]
   (local-time (new DateTime)))
  ([^js/DateTime now]
   (let [gtime (.. now (toIsoString true))]
     (str/replace gtime " " "T"))))

;(def template "https://hive-6c54a.appspot.com/directions/v5")
(def template "http://192.168.0.45:3000/area/frankfurt/directions?coordinates={coordinates}&departure={}")

(defn directions
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [coordinates departure]
  (let [url (-> (str/replace template "{coordinates}" coordinates)
                (str/replace "{departure}" (local-time departure)))]
    [url {:method "GET"
          :headers {:Accept "application/json"}}]))
