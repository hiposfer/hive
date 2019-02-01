(ns hive.screens.home.directions.handlers
  (:require [hive.utils.miscelaneous :as misc]
            [goog.date.duration :as duration]
            [datascript.core :as data]
            [hive.queries :as queries]
            [hive.services.kamal :as kamal]))

(defn walk-message
  [steps]
  (let [distance (apply + (map :step/distance steps))
        departs  (:step/arrive (first steps))
        arrive   (:step/arrive (last steps))
        duration (duration/format (* 1000 (- arrive departs)))]
    (str "walk " (misc/convert distance :from "meters" :to "km")
         " km (around " duration ").")))

(defn previous-routes
  [db]
  (let [current-route (data/entity db (data/q queries/user-route db))]
    (for [datom (data/datoms db :avet :directions/uuid)
          :let [route (data/entity db (:e datom))]
          :when (< (:directions/uuid route)
                   (:directions/uuid current-route))]
      route)))

(defn next-routes
  [db]
  (let [current-route (data/entity db (data/q queries/user-route db))]
    (for [datom (data/datoms db :avet :directions/uuid)
          :let [route (data/entity db (:e datom))]
          :when (< (:directions/uuid current-route)
                   (:directions/uuid route))]
      route)))

(defn on-previous-pressed
  [db]
  (let [user-id  (data/q queries/user-entity db)
        user     (data/entity db user-id)
        previous (previous-routes db)]
    (when (some? (last previous))
      [{:user/uid        (:user/uid user)
        :user/directions [:directions/uuid (:directions/uuid (last previous))]}])))

(defn on-next-pressed
  [db]
  (let [user-id  (data/q queries/user-entity db)
        user     (data/entity db user-id)
        start    (:coordinates (:geometry (:user/position user)))
        end      (:coordinates (:place/geometry (:user/goal user)))
        steps    (eduction (map :step/wait)
                           (remove nil?)
                           (:directions/steps (:user/directions user)))
        can-wait (first steps)
        departs  (+ (js/Date.now) (* 1000 can-wait) 1000)
        nexts    (next-routes db)]
    (if (some? (first nexts))
      [{:user/uid (:user/uid user)
        :user/directions [:directions/uuid (:directions/uuid (first nexts))]}]
      [[kamal/get-directions! db [start end] departs]])))

(defn clean-directions
  "remove all directions downloaded on this screen to avoid displaying them
  if the user goes back and forth between goal selection and directions screen"
  [db]
  (for [r (data/q queries/routes-ids db)]
    [:db.fn/retractEntity [:directions/uuid r]]))
