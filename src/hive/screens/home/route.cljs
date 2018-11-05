(ns hive.screens.home.route
  (:require [hive.utils.miscelaneous :as tool]
            [hive.screens.symbols :as symbols]
            [react-native :as React]
            [expo :as Expo]
            [hive.utils.geometry :as geometry]
            [goog.date.duration :as duration]
            [reagent.core :as r]
            [clojure.string :as str]
            [hive.assets :as assets]
            [hive.state.core :as state]))

(def big-circle 16)
(def small-circle 10)
(def micro-circle 3)
(def section-height 140)
(def subsection-height 20)

(def default-color "#3bb2d0")

;; adapted from
;; https://stackoverflow.com/a/16348977
(defn color
  [text]
  (let [h (reduce (fn [res i] (+ (. text (charCodeAt i))
                                 (- (bit-shift-left res 5)
                                    res)))
                  0
                  (range (count text)))]
    (reduce (fn [res i]
              (let [v (bit-and (bit-shift-right h (* i 8)) 0xFF)]
                (str res (. (str "00" (. v (toString 16)))
                            (substr -2)))))
            "#"
            (range 3))))

(defn- route-color
  [route]
  (or (:route/color route)
      (when (some? route)
        (color (or (:route/short_name route) (:route/long_name route))))
      default-color))

(defn- TransitLine
  [steps expanded?]
  (let [line-height (if expanded? "90%" "80%")
        stroke      (route-color (:trip/route (:step/trip (first steps))))]
    [:> React/View {:flex 1 :alignItems "center"}
      [:> React/View (merge {:backgroundColor stroke}
                            (symbols/circle big-circle))]
      [:> React/View {:backgroundColor stroke :width "8%" :height line-height}]
      [:> React/View (merge {:style {:backgroundColor stroke :borderColor "transparent"}}
                            (symbols/circle big-circle))]]))

(defn- WalkingSymbols
  [steps expanded?]
  (let [amount  (if (not expanded?) 15 (/ section-height 4))]
    [:> React/View {:flex 1 :alignItems "center"
                    :justifyContent "space-around"}
      (for [i (range amount)]
        ^{:key i}
        [:> React/View (merge (symbols/circle micro-circle)
                              {:backgroundColor "slategray"})])]))

#_(defn- onStopPress
    [props step]
    (let [navigate (:navigate (:navigation props))
          stop     (select-keys (:stop_times/stop step) [:stop/id])]
      [[navigate "gtfs" stop]
       [kamal/entity! stop]]))

(defn- StepDetails
  [props steps]
  (into [:> React/View {:style {:flex 9 :justifyContent "space-around"}}]
    (for [step steps]
      (if (= "transit" (:step/mode step))
        ;[:> React/TouchableOpacity {:onPress #(state/transact! (onStopPress props step))}
        [:> React/Text {:style {:color "gray"}} (:step/name step)]
        [:> React/Text {:style {:color "gray"}}
                       (str/replace (:maneuver/instruction (:step/maneuver step))
                                    "[Dummy]" "")]))))

(defn- StepOverview
  [props steps expanded?]
  [:> React/View {:flex 9 :justifyContent "space-between"}
    [:> React/Text {:style {:flex 1}} (some :step/name (butlast steps))]
    [:> React/TouchableOpacity {:style {:flex (if @expanded? 2 5)
                                        :justifyContent "center"}
                                :onPress #(reset! expanded? (not @expanded?))}
      [:> React/View {:flex-direction "row"}
        [:> assets/Ionicons {:name  (if @expanded? "ios-arrow-down" "ios-arrow-forward")
                             :style {:paddingRight 10}
                             :size  22 :color "gray"}]
        [:> React/Text {:style {:color "gray" :paddingRight 7}}
                       (str/replace (:maneuver/instruction (:step/maneuver (first steps)))
                                    "[Dummy]" "")]]]
    (when @expanded?
      [:> React/View {:flex 5}
        [StepDetails props (butlast (rest steps))]])
    [:> React/Text {:style {:flex 1}} (:step/name (last steps))]])

(defn- RouteSection
  [props steps human-time]
  (r/with-let [expanded? (r/atom false)]
    (let [subsize (* subsection-height (count steps))
          height  (+ section-height (if @expanded? subsize 0))]
      [:> React/View {:height height :flexDirection "row"}
        [:> React/Text {:style {:flex 1 :textAlign "right"
                                :color "gray" :fontSize 12}}
                      human-time]
        (if (= "walking" (:step/mode (first steps)))
          [WalkingSymbols steps @expanded?]
          [TransitLine steps @expanded?])
        [StepOverview props steps expanded?]])))

(defn- Route
  [props uid]
  (let [route   @(state/pull! [{:directions/steps
                                [:step/arrive
                                 :step/mode
                                 :step/name
                                 {:step/maneuver [:maneuver/instruction]}
                                 :step/distance
                                 {:step/trip [{:trip/route [:route/long_name :route/color]}]}]}]
                              [:directions/uuid uid])]
    [:> React/View {:flex 1}
      (for [steps (partition-by :step/mode (:directions/steps route))
            :let [arrives  (:step/arrive (first steps))
                  iso-time (.toLocaleTimeString (new js/Date (* 1000 arrives)) "de-De")
                  human-time (subs iso-time 0 5)]]
        ^{:key arrives}
        [RouteSection props steps human-time])]))

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
                              [:step/geometry :step/mode
                               {:step/trip [{:trip/route [:route/color :route/long_name]}]}]}]
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
