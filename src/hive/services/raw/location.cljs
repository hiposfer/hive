(ns hive.services.raw.location
  (:require [hive.rework.core :as work]
            [hive.foreigns :as fl]
            [cljs.core.async :as async]
            [hive.rework.util :as tool]
            [hive.queries :as queries]
            [cljs.spec.alpha :as s]
            [oops.core :as oops]
            [datascript.core :as data]))

(s/def ::enableHighAccuracy boolean?)
(s/def ::timeInterval (s/and number? pos?))
(s/def ::distanceInterval (s/and number? pos?))
(s/def ::callback ifn?)

(s/def ::opts (s/keys :req [::callback]
                      :opt [::enableHighAccuracy ::timeInterval
                            ::distanceInterval]))

;; todo: should altitude be inside the point coordinates?
(defn point
  "transform an Expo location format into a GeoJson Feature Point"
  [expo-loc]
  (let [point {:type "Point"
               :coordinates [(:longitude (:coords expo-loc))
                             (:latitude  (:coords expo-loc))]}
        ncords (dissoc (:coords expo-loc) :latitude :longitude)]
    {:type "Feature" :geometry point
     :properties (merge ncords (dissoc expo-loc :coords))}))

(defn- set-watcher
  [data]
  [{:session/uuid                   (:session/uuid data)
    :app.location.watcher/ref       (::watcher data)
    :app.location.watcher/timestamp (js/Date.now)}])

(defn watch!
  "watch the user location. Receives an options object according to
  Expo's API: https://docs.expo.io/versions/latest/sdk/location.html"
  [opts]
  (if (and (= "android" (oops/oget fl/ReactNative "Platform.OS"))
           (not (oops/oget fl/Expo "Constants.isDevice")))
    (let [msg "Oops, this will not work on Sketch in an Android emulator. Try it on your device!"]
      (async/to-chan [(ex-info msg (assoc opts ::reason ::emulator-denial))]))
    (let [js-opts (clj->js opts)
          session (data/q queries/session (work/db))
          request (fn [response]
                    (if (not= (:status response) "granted")
                      (ex-info "permission denied" (assoc response ::reason ::permission-denied))
                      {::watcher (oops/ocall fl/Expo "Location.watchPositionAsync"
                                             js-opts (::callback opts))}))]
      ;; convert promise to channel and execute it
      (tool/async (oops/ocall fl/Expo "Permissions.askAsync" "location")
                  (map tool/keywordize)
                  (map request)
                  tool/bypass-error
                  (map #(assoc % :session/uuid session))
                  (map set-watcher)))))

(defn stop!
  "stop watching the user location if a watcher was set before"
  [query]
  (let [sub (data/q query (work/db)) ;; HACK: but it gets the job done
        f   (:remove sub)]
    (when f ;;todo: is it necessary to remove it from the state?
      (f))))

(s/fdef watch! :args (s/cat :options ::opts))

;(async/take! (watch! {:enableHighAccuracy true :timeInterval 3000})
;             tool/log]])
