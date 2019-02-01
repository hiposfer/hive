(ns hive.screens.home.directions.ui
  "The Directions Screen is where the user can see details about a route
   returned by Kamal

   It features a full screen map with a details section below containing
   the steps required to reach the goal"
  (:require [react-native :as React]
            [expo :as Expo]
            [reagent.core :as r]
            [datascript.core :as data]
            [goog.date.duration :as duration]
            [clojure.string :as str]
            [hive.assets :as assets]
            [hive.state :as state]
            [hive.queries :as queries]
            [hive.services.kamal :as kamal]
            [hive.utils.miscelaneous :as misc]
            [hive.utils.geometry :as geometry]
            [hive.screens.symbols :as symbols]
            [hive.screens.home.directions.trip.ui :as trip.ui])
  (:import (goog.date DateTime)))

(def micro-circle 3)

(defn- WalkingDots
  [steps]
  (let [style      (merge (symbols/circle micro-circle)
                          {:backgroundColor "slategray"})]
    [:> React/View {:width 20 :alignItems "center"
                    :justifyContent "space-around"}
      (when (= "depart" (:maneuver/type (:step/maneuver (first steps))))
        [:> React/View (merge (symbols/circle trip.ui/big-circle)
                              {:backgroundColor "slategray"})])
      (for [i (range 10)] ^{:key i} [:> React/View style])
      (when (= "arrive" (:maneuver/type (:step/maneuver (last steps))))
        [:> React/View (merge (symbols/circle trip.ui/big-circle)
                              {:backgroundColor "slategray"})])]))

(defn- walk-message
  [steps]
  (let [distance (apply + (map :step/distance steps))
        departs  (:step/arrive (first steps))
        arrive   (:step/arrive (last steps))
        duration (duration/format (* 1000 (- arrive departs)))]
    (str "walk " (misc/convert distance :from "meters" :to "km")
         " km (around " duration ").")))

(defn- SectionIcon
  [steps]
  (if (= "walking" (:step/mode (first steps)))
    [:> assets/Ionicons {:name "ios-walk" :size 32}]
    [trip.ui/TripIcon (:step/trip (first steps))]))

(defn- StepOverviewMsg
  [props steps]
  (let [navigate (:navigate (:navigation props))
        trip     (:step/trip (first steps))]
    [:> React/TouchableOpacity {:style {:flex 1 :justifyContent "center"}
                                :onPress #(state/transact! [[navigate "trip" trip]
                                                            [kamal/get-trip-details! (state/db)
                                                                                     "2018-05-07T10:15:30+01:00"
                                                                                     (:trip/id trip)]])}
      [:> React/View {:flex-direction "row" :alignItems "center"}
        [:> React/View {:paddingRight 10 :with 32}
          [SectionIcon steps]]
        [:> React/Text {:style {:color "gray" :paddingRight 7 :flex 3}}
          (if (= "walking" (:step/mode (first steps)))
            (walk-message steps)
            (-> (:maneuver/instruction (:step/maneuver (first steps)))
                (str/replace "[Dummy]" "")
                (subs 0 60)))]
        [:> assets/Ionicons {:name "ios-arrow-forward" :size 22
                             :color "gray" :style {:paddingRight 20}}]]]))

(defn- StepOverview
  [props steps]
  [:> React/View {:flex 1 :justifyContent "space-between"}
    (when (or (= "transit" (:step/mode (first steps)))
              (= "depart" (:maneuver/type (:step/maneuver (first steps)))))
      [:> React/Text {:style {:height 20}}
                     (some :step/name (butlast steps))])
    [StepOverviewMsg props steps]
    (when (or (= "transit" (:step/mode (first steps)))
              (= "arrive" (:maneuver/type (:step/maneuver (last steps)))))
      [:> React/Text {:style {:height 20}}
                     (:step/name (last steps))])])

(defn- RouteSectionTimes
  [props steps]
  [:> React/View {:width 40 :justifyContent "space-between"}
    [:> React/Text {:style trip.ui/time-style}
      (when (or (= "transit" (:step/mode (first steps)))
                (= "depart" (:maneuver/type (:step/maneuver (first steps)))))
        (misc/hour-minute (:step/arrive (first steps))))]
    (when (or (= "transit" (:step/mode (last steps)))
              (= "arrive" (:maneuver/type (:step/maneuver (last steps)))))
      [:> React/Text {:style trip.ui/time-style}
                     (misc/hour-minute (:step/arrive (last steps)))])])

(defn- RouteSection
  [props steps]
  (let [height     (get trip.ui/section-height (:step/mode (first steps)))]
    [:> React/View {:height height :flexDirection "row"}
      [RouteSectionTimes props steps]
      (if (= "walking" (:step/mode (first steps)))
        [WalkingDots steps]
        [trip.ui/TransitLine steps])
      [StepOverview props steps]]))

(defn- Route
  [props user-route]
  (let [route   @(state/pull! [{:directions/steps
                                [:step/arrive
                                 :step/mode
                                 :step/name
                                 {:step/maneuver [:maneuver/instruction
                                                  :maneuver/type]}
                                 :step/distance
                                 {:step/trip [:trip/id
                                              {:trip/route [:route/long_name
                                                            :route/short_name
                                                            :route/type
                                                            :route/color]}]}]}]
                              user-route)]
    [:> React/View {:flex 1}
      (for [steps (partition-by :step/mode (:directions/steps route))]
        ^{:key (:step/arrive (first steps))}
        [RouteSection props steps])]))

(defn- Transfers
  [user-route]
  (let [route @(state/pull! [:directions/duration
                             {:directions/steps
                              [:step/arrive
                               :step/mode
                               {:step/trip [{:trip/route [:route/type]}]}]}]
                            user-route)
        sections (partition-by :step/mode (:directions/steps route))]
    [:> React/View {:width "40%" :flexDirection "row" :justifyContent "flex-start"
                    :alignItems "center" :padding 5}
      (butlast ;; drop last arrow icon
        (interleave
          (for [steps sections]
            ^{:key (:step/arrive (first steps))}
            [SectionIcon steps])
          (for [i (range (count sections))]
            ^{:key i}
            [:> assets/Ionicons {:name "ios-arrow-forward" :size 22 :color "gray"
                                 :style {:paddingHorizontal 10}}])))
     [:> React/Text {:style {:color "gray" :paddingTop "2.5%" :paddingLeft "10%"
                             :paddingRight 25}}
       (duration/format (* 1000 (:directions/duration route)))]]))

(defn- previous-routes
  [db]
  (let [current-route (data/entity db (data/q queries/user-route db))]
    (for [datom (data/datoms db :avet :directions/uuid)
          :let [route (data/entity db (:e datom))]
          :when (< (:directions/uuid route)
                   (:directions/uuid current-route))]
      route)))

(defn- next-routes
  [db]
  (let [current-route (data/entity db (data/q queries/user-route db))]
    (for [datom (data/datoms db :avet :directions/uuid)
          :let [route (data/entity db (:e datom))]
          :when (< (:directions/uuid current-route)
                   (:directions/uuid route))]
      route)))

(defn- on-previous-pressed
  [db]
  (let [user-id  (data/q queries/user-entity db)
        user     (data/entity db user-id)
        previous (previous-routes db)]
    (when (some? (last previous))
      [{:user/uid        (:user/uid user)
        :user/directions [:directions/uuid (:directions/uuid (last previous))]}])))

(defn- on-next-pressed
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

(defn- Info
  [props user-route]
  ;; check if there are any previous directions that we can go back to
  (let [route    @(state/pull! [{:directions/steps [:step/arrive]}]
                               user-route)
        previous  (previous-routes (state/db))
        alignment (if (not-empty previous) "space-between" "flex-end")
        ;; goal - cannot change during this component lifetime
        goal      (data/entity (state/db) (data/q queries/user-goal (state/db)))]
    [:> React/View props
      [:> React/View {:flexDirection "row" :paddingLeft "1.5%"
                      :justifyContent "space-between"}
       [Transfers user-route]
       [:> React/View {:paddingRight 20}
         [:> React/Text {:style {:color "gray"}}
           (misc/hour-minute (:step/arrive (first (:directions/steps route))))]
         [:> React/Text {:style {:color "gray"}}
           (misc/hour-minute (:step/arrive (last (:directions/steps route))))]]]
      [:> React/Text {:style {:color "gray" :paddingLeft "2.5%"}} (:place/text goal)]
      [:> React/View {:flexDirection "row" :justifyContent alignment}
        (when (not-empty previous)
          [:> React/TouchableOpacity {:style {:flexDirection "row" :padding 5
                                              :alignItems "center"}
                                      :onPress #(state/transact! (on-previous-pressed (state/db)))}
            [:> assets/Ionicons {:name "ios-arrow-back" :size 22 :color "#3bb2d0"
                                 :style {:paddingRight 5}}]
            [:> React/Text {:style {:color "#3bb2d0" :fontSize 18}}
                           "previous"]])
        [:> React/TouchableOpacity {:style {:flexDirection "row" :padding 5
                                            :alignItems "center" :paddingLeft 40}
                                    :onPress #(state/transact! (on-next-pressed (state/db)))}
          [:> React/Text {:style {:color "#3bb2d0" :fontSize 18}}
                         "next"]
          [:> assets/Ionicons {:name "ios-arrow-forward" :size 22 :color "#3bb2d0"
                               :style {:paddingLeft 5}}]]]]))


(defn- MapLines
  [user-route]
  (let [route @(state/pull! [{:directions/steps
                              [:step/geometry
                               :step/mode
                               {:step/trip
                                [{:trip/route [:route/color
                                               :route/long_name
                                               :route/short_name]}]}]}]
                            user-route)]
    (for [steps (partition-by :step/mode (:directions/steps route))
          :let [coords (mapcat :coordinates (map :step/geometry steps))
                stroke (misc/route-color (:trip/route (:step/trip (first steps))))]]
      ^{:key (hash steps)}
      [:> Expo/MapView.Polyline {:coordinates (map geometry/latlng coords)
                                 :strokeColor stroke
                                 :strokeWidth 4}])))

(defn- clean-directions
  "remove all directions downloaded on this screen to avoid displaying them
  if the user goes back and forth between goal selection and directions screen"
  [db]
  (for [r (data/q queries/routes-ids db)]
    [:db.fn/retractEntity [:directions/uuid r]]))

(defn Screen
  [props]
  (r/with-let [window       (misc/keywordize (React/Dimensions.get "window"))
               route        (state/q! queries/user-route)
               bbox         (state/q! queries/user-area-bbox)
               position     (state/q! queries/user-position)
               back-handler (React/BackHandler.addEventListener
                              "hardwareBackPress"
                              #(misc/nullify (state/transact! (clean-directions (state/db)))))]
    [:> React/ScrollView {:flex 1}
     [:> React/View {:height (* 0.85 (:height window))}
      (let [children (when (some? @route) {:children (MapLines @route)})
            area     (geometry/mapview-region (merge children {:bbox     @bbox
                                                               :position @position}))]
        [:> Expo/MapView {:region                area
                          :showsUserLocation     true
                          :style                 {:flex 1}
                          :showsMyLocationButton true}
         (:children children)])]
     [:> React/View {:flex        1 :backgroundColor "white" :elevation 25
                     :shadowColor "#000000" :shadowRadius 25 :shadowOpacity 1.0}
      (when (some? @route)
        [Info {:height (* 0.15 (:height window)) :paddingTop "1%"} @route])
      (when (some? @route)
        [Route props @route])]]
    (finally (. back-handler (remove)))))
