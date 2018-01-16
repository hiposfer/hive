(ns hive.services.location
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

(defn- tx-position
  [data]
  [{:user/id (:user/id data)
    :user/position (dissoc data :user/id)}])

(def update-position (comp tx-position
                           #(rework/inject % :user/id queries/user-id)
                           tool/keywordize))

(defn- watch
  [opts]
  (if (and (= "android" (:OS fl/Platform))
           (not (:isDevice fl/Constants)))
    (ex-info "Oops, this will not work on Sketch in an Android emulator. Try it on your device!"
             {} ::emulator-denial)
    (let [result (async/chan)
          js-opts (clj->js opts)]
      (-> ((:askAsync fl/Permissions) (:LOCATION fl/Permissions))
          (.then tool/keywordize)
          (.then #(when (not= (:status %) "granted")
                    (async/put! result
                      (ex-info "permission denied" % ::permission-denied))))
          (.then #((:watchPositionAsync fl/Location) js-opts (::callback opts)))
          (.then #(async/put! result %))
          (.catch #(async/put! result
                     (ex-info "no location provider" % ::no-location-provider))))
      result)))

(defn- set-watcher
  [data]
  [{:app/session (:app/session data)
    :app.location/watcher (::watcher data)}])

(def watch!
  "watch the user location. Receives an options object according to
  Expo's API: https://docs.expo.io/versions/latest/sdk/location.html"
  (rework/pipe watch
               tool/keywordize
               #(hash-map ::watcher %)
               #(rework/inject % :app/session queries/session)
               set-watcher))

(defn stop!
  "stop watching the user location if a watcher was set before"
  [query]
  (let [sub (rework/q query)
        f (:remove sub)]
    (when f ;;todo: is it necessary to remove it from the state?
      (f))))

(s/fdef watch! :args (s/cat :options ::opts))

;(async/take! (watch! {:enableHighAccuracy true :timeInterval 3000})
;             tool/log]])
