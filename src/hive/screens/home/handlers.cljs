(ns hive.screens.home.handlers
  (:require [hive.services.kamal :as kamal]
            [datascript.core :as data]
            [hive.queries :as queries]
            [react-native :as React]
            [hive.utils.miscelaneous :as tool]
            [hive.services.mapbox :as mapbox]
            [hive.utils.promises :as promise]
            [hive.state :as state]))

(defn set-target
  "associates a target and a path to get there with the user"
  [db navigate target]
  (let [user     (data/q queries/user-id db)
        position (data/pull db [:user/position] [:user/uid user])
        start    (:coordinates (:geometry (:user/position position)))
        end      (:coordinates (:place/geometry target))]
    [{:user/uid  user
      :user/goal [:place/id (:place/id target)]}
     [kamal/get-directions! db [start end]]
     [React/Keyboard.dismiss]
     [navigate "directions"]]))

(defn humanize-distance
  "Convert a distance (meters) to human readable form."
  [distance]
  (if (> distance 1000)
    (str (. (/ distance 1000) (toFixed 1)) " km")
    (str (. distance (toFixed 0)) " m")))

(defn reset-places
  "transact the geocoding result under the user id"
  ([db]
   (for [id (data/q queries/places-id db)]
     [:db.fn/retractEntity id]))
  ([data db]
   (concat (reset-places db)
           (for [f (:features data)]
             (tool/with-ns "place" f)))))

(defn autocomplete
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  [text db]
  (let [user-id (data/q queries/user-entity db)
        network (data/q '[:find ?connection .
                          :where [?session :session/uuid]
                          [?session :connection/type ?connection]])
        user    (data/entity db user-id)
        args    {:query        text
                 :proximity    (:user/position user)
                 :access_token (:ENV/MAPBOX state/tokens)
                 :bbox         (:area/bbox (:user/area user))}]
    (cond
      (empty? text) nil

      ;; note - we assume that no position means no GPS enabled ...
      (not (some? (:user/position user)))
      [{:error/id :home/search
        :error/type :location/unknown}
       [React/Keyboard.dismiss]]

      (= "none" network)
      [{:error/id :home/search
        :error/type :internet/missing}
       [React/Keyboard.dismiss]]

      :else
      [[promise/finally [mapbox/geocoding! args]
                        [reset-places db]]])))
