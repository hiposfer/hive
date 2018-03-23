(ns hive.services.raw.location
  (:require [hive.rework.core :as rework]
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
                           (rework/inject :user/id queries/user-id)
                           point
                           tool/keywordize))

(defn- watch
  [xform]
  (let [result (async/chan 1 (comp tool/bypass-error xform))]
    (-> ((:askAsync fl/Permissions) (:LOCATION fl/Permissions))
        (.then #(do (async/put! result %) (async/close! result)))
        (.catch #(async/put! result %)))
    result))

(defn- set-watcher
  [data]
  [{:app/session (:app/session data)
    :app.location/watcher (::watcher data)}])

(defn watch!
  "watch the user location. Receives an options object according to
  Expo's API: https://docs.expo.io/versions/latest/sdk/location.html"
  [opts]
  (if (and (= "android" (:OS fl/Platform)) (not (:isDevice fl/Constants)))
    (let [msg "Oops, this will not work on Sketch in an Android emulator. Try it on your device!"]
      (async/to-chan [(ex-info msg opts ::emulator-denial)]))
    (let [js-opts (clj->js opts)
          xform  (comp (map tool/keywordize)
                       (map #(when (not= (:status %) "granted")
                               (ex-info "permission denied" % ::permission-denied)))
                       tool/bypass-error
                       (map #((:watchPositionAsync fl/Location) js-opts
                                                                (::callback opts)))
                       (map #(hash-map ::watcher %))
                       (map (rework/inject :app/session queries/session))
                       (map set-watcher))]
      (watch xform))))

(defn stop!
  "stop watching the user location if a watcher was set before"
  [query]
  (let [sub (rework/q query)
        f   (:remove sub)]
    (when f ;;todo: is it necessary to remove it from the state?
      (f))))

(s/fdef watch! :args (s/cat :options ::opts))

;(async/take! (watch! {:enableHighAccuracy true :timeInterval 3000})
;             tool/log]])
