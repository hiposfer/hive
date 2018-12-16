(ns hive.screens.home.directions
  (:require [hive.utils.miscelaneous :as tool]
            [hive.screens.symbols :as symbols]
            [react-native :as React]
            [expo :as Expo]
            [hive.utils.geometry :as geometry]
            [goog.date.duration :as duration]
            [clojure.string :as str]
            [hive.assets :as assets]
            [hive.state.core :as state]
            [hive.utils.miscelaneous :as misc]))

(def big-circle 16)
(def micro-circle 3)
(def section-height {"walking" 90 "transit" 120})

(def default-color "#3bb2d0")

(defn- route-color
  [route]
  (or (:route/color route)
      (when (some? route)
        (misc/color (str (or (:route/long_name route)
                             (:route/short_name route)))))
      default-color))

(defn- TransitLine
  [steps]
  (let [stroke      (route-color (:trip/route (:step/trip (first steps))))]
    [:> React/View {:width 20 :alignItems "center"}
      [:> React/View (merge {:backgroundColor stroke}
                            (symbols/circle big-circle))]
      [:> React/View {:backgroundColor stroke :width "15%" :flex 1}]
      [:> React/View (merge {:backgroundColor stroke :borderColor "transparent"}
                            (symbols/circle big-circle))]]))

(defn- WalkingDots
  [steps]
  (let [style      (merge (symbols/circle micro-circle)
                          {:backgroundColor "slategray"})]
    [:> React/View {:width 20 :alignItems "center"
                    :justifyContent "space-around"}
      (when (= "depart" (:maneuver/type (:step/maneuver (first steps))))
        [:> React/View (merge (symbols/circle big-circle)
                              {:backgroundColor "slategray"})])
      (for [i (range 10)] ^{:key i} [:> React/View style])
      (when (= "arrive" (:maneuver/type (:step/maneuver (last steps))))
        [:> React/View (merge (symbols/circle big-circle)
                              {:backgroundColor "slategray"})])]))

(defn- walk-message
  [steps]
  (let [distance (apply + (map :step/distance steps))
        departs  (:step/arrive (first steps))
        arrive   (:step/arrive (last steps))
        duration (duration/format (* 1000 (- arrive departs)))]
    (str "walk " (misc/convert distance :from "meters" :to "km")
         " km (around " duration ").")))

(defn- StepOverviewMsg
  [props steps]
  [:> React/TouchableOpacity {:style {:flex 1 :justifyContent "center"}}
    [:> React/View {:flex-direction "row" :alignItems "center"}
      [:> assets/Ionicons {:name "ios-arrow-forward" :style {:paddingRight 10}
                           :size 22 :color "gray"}]
      [:> React/Text {:style {:color "gray" :paddingRight 7}}
        (if (= "walking" (:step/mode (first steps)))
          (walk-message steps)
          (-> (:maneuver/instruction (:step/maneuver (first steps)))
              (str/replace "[Dummy]" "")))]]])

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

(def time-style {:textAlign "center" :color "gray" :fontSize 12})

(defn- RouteSectionTimes
  [props steps]
  (let [departs      (:step/arrive (first steps))
        arrives      (:step/arrive (last steps))
        departs-time (.toLocaleTimeString (new js/Date (* 1000 departs))
                                          "de-De")
        arrives-time (.toLocaleTimeString (new js/Date (* 1000 arrives))
                                          "de-De")]
    [:> React/View {:width 40 :justifyContent "space-between"}
      [:> React/Text {:style time-style}
        (when (or (= "transit" (:step/mode (first steps)))
                  (= "depart" (:maneuver/type (:step/maneuver (first steps)))))
          (subs departs-time 0 5))]
      (when (or (= "transit" (:step/mode (last steps)))
                (= "arrive" (:maneuver/type (:step/maneuver (last steps)))))
        [:> React/Text {:style time-style}
                       (subs arrives-time 0 5)])]))

(defn- RouteSection
  [props steps]
  (let [height     (get section-height (:step/mode (first steps)))]
    [:> React/View {:height height :flexDirection "row"}
      [RouteSectionTimes props steps]
      (if (= "walking" (:step/mode (first steps)))
        [WalkingDots steps]
        [TransitLine steps])
      [StepOverview props steps]]))

(defn- Route
  [props uid]
  (let [route   @(state/pull! [{:directions/steps
                                [:step/arrive
                                 :step/mode
                                 :step/name
                                 {:step/maneuver [:maneuver/instruction
                                                  :maneuver/type]}
                                 :step/distance
                                 {:step/trip [{:trip/route [:route/long_name
                                                            :route/short_name
                                                            :route/color]}]}]}]
                              [:directions/uuid uid])]
    [:> React/View {:flex 1}
      (for [steps (partition-by :step/mode (:directions/steps route))]
        ^{:key (:step/arrive (first steps))}
        [RouteSection props steps])]))

(defn- SectionIcon
  [steps]
  (if (= "walking" (:step/mode (first steps)))
    [:> assets/Ionicons {:name "ios-walk" :size 32}]
    (case (:route/type (:trip/route (:step/trip (first steps))))
      0 [:> assets/Ionicons {:name "ios-train" :size 32}]
      1 [:> assets/Ionicons {:name "ios-subway" :size 32}]
      2 [:> assets/Ionicons {:name "md-train" :size 32}]
      3 [:> assets/Ionicons {:name "ios-bus" :size 32}]
      ;; default
      [:> React/ActivityIndicator])))

(defn- Transfers
  [route-id]
  (let [route @(state/pull! [{:directions/steps
                              [:step/arrive
                               :step/mode
                               {:step/trip [{:trip/route [:route/type]}]}]}]
                            [:directions/uuid route-id])
        sections (partition-by :step/mode (:directions/steps route))]
    [:> React/View {:flex 4 :flexDirection "row" :justifyContent "space-around"
                    :top "0.5%" :alignItems "center"}
      (butlast ;; drop last arrow icon
        (interleave
          (for [steps sections]
            ^{:key (:step/arrive (first steps))}
            [SectionIcon steps])
          (for [i (range (count sections))]
            ^{:key i}
            [:> assets/Ionicons {:name "ios-arrow-forward" :size 22 :color "gray"}])))]))


(defn- Info
  [props route-id]
  (let [[duration goal] @(state/q! '[:find [?duration ?goal]
                                     :where [_      :user/directions ?route]
                                            [?route :directions/duration ?duration]
                                            [_      :user/goal ?target]
                                            [?target :place/text ?goal]])]
    [:> React/View props
      [:> React/View {:flexDirection "row" :paddingLeft "1.5%"}
        [Transfers route-id]
        [:> React/Text {:style {:flex 5 :color "gray" :paddingTop "2.5%"
                                :paddingLeft "10%"}}
          (when (some? duration)
            (duration/format (* 1000 duration)))]]
      [:> React/Text {:style {:color "gray" :paddingLeft "2.5%"}}
                     goal]]))

(defn- paths
  [uid]
  (let [route @(state/pull! [{:directions/steps
                              [:step/geometry
                               :step/mode
                               {:step/trip
                                [{:trip/route [:route/color
                                               :route/long_name
                                               :route/short_name]}]}]}]
                            [:directions/uuid uid])]
    (for [steps (partition-by :step/mode (:directions/steps route))
          :let [coords (mapcat :coordinates (map :step/geometry steps))
                stroke (route-color (:trip/route (:step/trip (first steps))))]]
      ^{:key (hash steps)}
      [:> Expo/MapView.Polyline {:coordinates (map geometry/latlng coords)
                                 :strokeColor stroke
                                 :strokeWidth 4}])))

(defn Instructions
  "basic navigation directions.

   Async, some data might be missing when rendered !!"
  [props]
  (let [window (tool/keywordize (React/Dimensions.get "window"))
        uid   @(state/q! '[:find ?uid .
                           :where [_      :user/directions ?route]
                                  [?route :directions/uuid ?uid]])]
    [:> React/ScrollView {:flex 1}
      [:> React/View {:height (* 0.9 (:height window))}
        [symbols/CityMap
          (when (some? uid)
            (paths uid))]]
      [:> React/View {:flex 1 :backgroundColor "white"}
        (when (some? uid)
          [Info {:flex 1 :paddingTop "1%"} uid])
        (when (some? uid)
          [Route props uid])]
      [:> React/View (merge (symbols/circle 52) symbols/shadow
                            {:position "absolute" :right "10%"
                             :top (* 0.88 (:height window))})
        [:> assets/Ionicons {:name "ios-navigate" :size 62 :color "blue"}]]]))

;hive.rework.state/conn
;(into {} (state/entity [:route/uuid #uuid"5b2d247b-f8c6-47f3-940e-dee71f97d451"]))
;(state/q queries/routes-ids)

;(let [id      (data/q queries/user-id (state/db))]
;  (:steps (:route/route (:user/route @(state/pull! [{:user/route [:route/route]}]
;                                                  [:user/id id])))))
