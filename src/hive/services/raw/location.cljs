(ns hive.services.raw.location
  (:require [hive.rework.core :as work]
            [hive.foreigns :as fl]
            [cljs.core.async :as async]
            [hive.rework.util :as tool]
            [hive.queries :as queries]
            [cljs.spec.alpha :as s]))

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

(defn- tx-position
  [data]
  [{:user/id (:user/id data)
    :user/position (dissoc data :user/id)}])

(def update-position (comp tx-position
                           (work/inject :user/id queries/user-id)
                           point
                           tool/keywordize))

(defn- set-watcher
  [data]
  [{:session/uuid                   (:session/uuid data)
    :app.location.watcher/ref       (::watcher data)
    :app.location.watcher/timestamp (js/Date.now)}])

(defn watch!
  "watch the user location. Receives an options object according to
  Expo's API: https://docs.expo.io/versions/latest/sdk/location.html"
  [opts]
  (if (and (= "android" (:OS fl/Platform)) (not (:isDevice fl/Constants)))
    (let [msg "Oops, this will not work on Sketch in an Android emulator. Try it on your device!"]
      (async/to-chan [(ex-info msg (assoc opts ::reason ::emulator-denial))]))
    (let [js-opts (clj->js opts)
          request (fn [response]
                    (let [data (tool/keywordize response)]
                      (if (not= (:status response) "granted")
                        (ex-info "permission denied" (assoc data ::reason ::permission-denied))
                        (let [wp  (:watchPositionAsync fl/Location)
                              ref (wp js-opts (::callback opts))]
                          {::watcher ref}))))]
      ;; convert promise to channel and execute it
      (tool/channel ((:askAsync fl/Permissions) (:LOCATION fl/Permissions))
                    (comp tool/bypass-error
                          (map request)
                          (map (work/inject :session/uuid queries/session))
                          (map set-watcher))))))

(defn stop!
  "stop watching the user location if a watcher was set before"
  [query]
  (let [sub (work/q query)
        f   (:remove sub)]
    (when f ;;todo: is it necessary to remove it from the state?
      (f))))

(s/fdef watch! :args (s/cat :options ::opts))

;(async/take! (watch! {:enableHighAccuracy true :timeInterval 3000})
;             tool/log]])
