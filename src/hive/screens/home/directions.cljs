(ns hive.screens.home.directions
  (:require [hive.utils.miscelaneous :as tool]
            [hive.screens.symbols :as symbols]
            [react-native :as React]
            [expo :as Expo]
            [reagent.core :as r]
            [hive.utils.geometry :as geometry]
            [goog.date.duration :as duration]
            [clojure.string :as str]
            [hive.assets :as assets]
            [hive.state.core :as state]
            [hive.utils.miscelaneous :as misc]
            [hive.state.queries :as queries]
            [datascript.core :as data]))

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

(defn- StepOverviewMsg
  [props steps]
  [:> React/TouchableOpacity {:style {:flex 1 :justifyContent "center"}}
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
                           :color "gray" :style {:paddingRight 20}}]]])

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
  [props user-route]
  (let [route   @(state/pull! [{:directions/steps
                                [:step/arrive
                                 :step/mode
                                 :step/name
                                 {:step/maneuver [:maneuver/instruction
                                                  :maneuver/type]}
                                 :step/distance
                                 {:step/trip [{:trip/route [:route/long_name
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
  (let [route @(state/pull! [{:directions/steps
                              [:step/arrive
                               :step/mode
                               {:step/trip [{:trip/route [:route/type]}]}]}]
                            user-route)
        sections (partition-by :step/mode (:directions/steps route))]
    [:> React/View {:width 100 :flexDirection "row" :justifyContent "flex-start"
                    :alignItems "center" :padding 5}
      (butlast ;; drop last arrow icon
        (interleave
          (for [steps sections]
            ^{:key (:step/arrive (first steps))}
            [SectionIcon steps])
          (for [i (range (count sections))]
            ^{:key i}
            [:> assets/Ionicons {:name "ios-arrow-forward" :size 22 :color "gray"
                                 :style {:paddingHorizontal 10}}])))]))


(defn- Info
  [props user-route]
  (let [route @(state/pull! [:directions/duration] user-route)
        goal  @(state/q! '[:find ?goal .
                           :where [_ :user/goal ?target]
                                  [?target :place/text ?goal]])]
    [:> React/View props
      [:> React/View {:flexDirection "row" :paddingLeft "1.5%" :justifyContent "space-between"}
       [Transfers user-route]
       [:> React/Text {:style {:color        "gray" :paddingTop "2.5%" :paddingLeft "10%"
                               :paddingRight 25}}
        (when (some? route)
          (duration/format (* 1000 (:directions/duration route))))]]
      [:> React/Text {:style {:color "gray" :paddingLeft "2.5%"}} goal]
      [:> React/View {:flexDirection "row" :justifyContent "space-between"}
        [:> React/View {:flexDirection "row" :padding 5 :alignItems "center"}
          [:> assets/Ionicons {:name "ios-arrow-back" :size 22 :color "#3bb2d0"
                               :style {:paddingRight 5}}]
          [:> React/Text {:style {:color "#3bb2d0" :fontSize 18}}
                         "previous"]]
        [:> React/View {:flexDirection "row" :padding 5 :alignItems "center"}
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
                stroke (route-color (:trip/route (:step/trip (first steps))))]]
      ^{:key (hash steps)}
      [:> Expo/MapView.Polyline {:coordinates (map geometry/latlng coords)
                                 :strokeColor stroke
                                 :strokeWidth 4}])))

(defn- clean-directions
  [db]
  (for [r (data/q queries/routes-ids db)]
    [:db.fn/retractEntity [:directions/uuid r]]))

(defn Instructions
  "basic navigation directions.

   Async, some data might be missing when rendered !!"
  [props]
  (r/with-let [window        (tool/keywordize (React/Dimensions.get "window"))
               route         (state/q! '[:find ?route .
                                         :where [_ :user/directions ?route]])
               bbox          (state/q! queries/user-area-bbox)
               position      (state/q! queries/user-position)
               back-handler  (React/BackHandler.addEventListener
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
      [:> React/View {:flex 1 :backgroundColor "white" :elevation 25
                      :shadowColor "#000000" :shadowRadius 25 :shadowOpacity 1.0}
        (when (some? @route)
          [Info {:height (* 0.15 (:height window)) :paddingTop "1%"} @route])
        (when (some? @route)
          [Route props @route])]]
    (finally (. back-handler (remove)))))

;(into {} (state/entity [:route/uuid #uuid"5b2d247b-f8c6-47f3-940e-dee71f97d451"]))
;(state/q queries/routes-ids)

;(let [id      (data/q queries/user-id (state/db))]
;  (:steps (:route/route (:user/route @(state/pull! [{:user/route [:route/route]}]
;                                                  [:user/id id])))))
