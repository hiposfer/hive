(ns hive.services.location
  (:require [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [hive.queries :as queries]
            [hive.rework.core :as work]
            [hive.foreigns :as fl]
            [hive.queries :as queries]
            [datascript.core :as data]))

;; todo: should altitude be inside the point coordinates?
(defn point
  "transform an Expo location format into a GeoJson Feature Point"
  [expo-loc]
  (let [point {:type "Point"
               :coordinates [(:longitude (:coords expo-loc))
                             (:latitude  (:coords expo-loc))]}
        ncords (dissoc (:coords expo-loc) :latitude :longitude)]
    {:type "Feature"
     :geometry point
     :properties (merge ncords (dissoc expo-loc :coords))}))

(defn set-location
  [db data]
  (let [p    (point (tool/keywordize data))
        uid  (data/q queries/user-id db)]
    [{:user/id uid
      :user/position p}]))

(defn defaults
  "sensitive defaults for location tracking"
  [callback]
  {:enableHighAccuracy true
   :timeInterval       3000
   :callback          callback})

(defn- request
  [opts response]
  (if (= (:status response) "granted")
    (.. fl/Expo (Location.watchPositionAsync (clj->js opts) (:callback opts)))
    (ex-info "permission denied" (assoc response ::reason ::permission-denied))))

(defn watch!
  "watch the user location. Receives an options object according to
  Expo's API: https://docs.expo.io/versions/latest/sdk/location.html

  Returns a promise that will resolve to the watchPositionAsync return value"
  ^js/Promise [opts]
  (if (and (= "android" (.. fl/ReactNative -Platform.OS))
           (not (.. fl/Expo -Constants.isDevice)))
    (ex-info "Oops, this will not work on Sketch in an Android emulator. Try it on your device!"
             (assoc opts ::reason ::emulator-denial))
    (.. fl/Expo  (Permissions.askAsync "location")
                 (then tool/keywordize)
                 (then #(request opts %))
                 (then tool/reject-on-error))))
