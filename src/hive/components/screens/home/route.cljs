(ns hive.components.screens.home.route
  (:require [hive.components.foreigns.react :as react]
            [hive.rework.util :as tool]
            [datascript.core :as data]
            [hive.rework.core :as work]
            [hive.components.symbols :as symbols]
            [hive.foreigns :as fl]
            [hive.components.foreigns.expo :as expo]
            [hive.libs.geometry :as geometry]
            [goog.date.duration :as duration]
            [reagent.core :as r]
            [clojure.string :as str])
  (:import [goog.date Interval DateTime]))

;(.. DateTime (fromRfc822String "2018-05-07T10:15:30"))

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
        stroke      (route-color (:trip/route (:stop_times/trip (first steps))))]
    [:> react/View {:flex 1 :alignItems "center"}
      [:> react/View (merge {:backgroundColor stroke}
                            (symbols/circle big-circle))]
      [:> react/View {:backgroundColor stroke :width "8%" :height line-height}]
      [:> react/View (merge {:style {:backgroundColor stroke :borderColor "transparent"}}
                            (symbols/circle big-circle))]]))

(defn- WalkingSymbols
  [steps expanded?]
  (let [amount  (if (not expanded?) 15 (/ section-height 4))]
    [:> react/View {:flex 1 :alignItems "center"
                    :justifyContent "space-around"}
      (for [i (range amount)]
        ^{:key i}
        [:> react/View (merge (symbols/circle micro-circle)
                              {:backgroundColor "slategray"})])]))


(defn- StepDetails
  [steps]
  [:> react/View {:style {:flex 9 :justifyContent "space-around"}}
    (for [step steps]
      ^{:key (hash step)}
      [:> react/Text {:style {:color "gray"}}
        (if (= "transit" (:step/mode step))
          (:step/name step)
          (str/replace (:maneuver/instruction step)
                       "[Dummy]" ""))])])

(defn- StepOverview
  [steps expanded?]
  [:> react/View {:flex 9 :justifyContent "space-between"}
    [:> react/Text {:style {:flex 1}} (some :step/name (butlast steps))]
    [:> react/TouchableOpacity {:style {:flex (if @expanded? 2 5)
                                        :justifyContent "center"}
                                :onPress #(reset! expanded? (not @expanded?))}
      [:> react/View {:flex-direction "row"}
        [:> expo/Ionicons {:name (if @expanded? "ios-arrow-down" "ios-arrow-forward")
                           :style {:paddingRight 10}
                           :size 22 :color "gray"}]
        [:> react/Text {:style {:color "gray" :paddingRight 7}}
                       (str/replace (:maneuver/instruction (first steps))
                                    "[Dummy]" "")]]]
    (when @expanded?
      [:> react/View {:flex 5}
        [StepDetails (butlast (rest steps))]])
    [:> react/Text {:style {:flex 1}} (:step/name (last steps))]])

(defn- RouteSection
  [steps human-time]
  (r/with-let [expanded? (r/atom false)]
    (let [subsize (* subsection-height (count steps))
          height  (+ section-height (if @expanded? subsize 0))]
      [:> react/View {:height height :flexDirection "row"}
        [:> react/Text {:style {:flex 1 :textAlign "right"
                                :color "gray" :fontSize 12}}
                      human-time]
        (if (= "walking" (:step/mode (first steps)))
          [WalkingSymbols steps @expanded?]
          [TransitLine steps @expanded?])
        [StepOverview steps expanded?]])))

(defn- Route
  [uid]
  (let [route   @(work/pull! [{:directions/steps [:step/departure :step/mode
                                                  :step/name :maneuver/instruction
                                                  :step/distance
                                                  {:stop_times/trip [{:trip/route [:route/long_name :route/color]}]}]}]
                             [:directions/uuid uid])]
    [:> react/View {:flex 1}
      (for [steps (partition-by :step/mode (:directions/steps route))
            :let [departs  (:step/departure (first steps))
                  iso-time (. ^DateTime departs (toIsoTimeString))
                  human-time (subs iso-time 0 5)]]
        ^{:key human-time}
        [RouteSection steps human-time])]))

(defn- SectionIcon
  [steps]
  (if (= "walking" (:step/mode (first steps)))
    [:> expo/Ionicons {:name "ios-walk" :size 32}]
    (case (:route/type (:trip/route (:stop_times/trip (first steps))))
      0 [:> expo/Ionicons {:name "ios-train" :size 32}]
      1 [:> expo/Ionicons {:name "ios-subway" :size 32}]
      2 [:> expo/Ionicons {:name "md-train" :size 32}]
      3 [:> expo/Ionicons {:name "ios-bus" :size 32}]
      ;; default
      [:> react/ActivityIndicator])))

(defn- Transfers
  [route-id]
  (let [route @(work/pull! [{:directions/steps [:step/mode {:stop_times/trip [{:trip/route [:route/type]}]}]}]
                           [:directions/uuid route-id])
        sections (partition-by :step/mode (:directions/steps route))]
    [:> react/View {:flex 4 :flexDirection "row" :justifyContent "space-around"
                    :top "0.5%" :alignItems "center"}
      (butlast ;; drop last arrow icon
        (interleave
          (for [steps sections]
            ^{:key (hash steps)}
            [SectionIcon steps])
          (for [i (range (count sections))]
            ^{:key i}
            [:> expo/Ionicons {:name "ios-arrow-forward" :size 22 :color "gray"}])))]))


(defn- Info
  [props route-id]
  (let [[duration goal] @(work/q! '[:find [?duration ?goal]
                                    :where [_      :user/directions ?route]
                                           [?route :directions/duration ?duration]
                                           [_      :user/goal ?target]
                                           [?target :place/text ?goal]])]
    [:> react/View props
     [:> react/View {:flexDirection "row" :paddingLeft "1.5%"}
      [Transfers route-id]
      [:> react/Text {:style {:flex 5 :color "gray" :paddingTop "2.5%"
                              :paddingLeft "10%"}}
       (when (some? duration)
         (duration/format (* 1000 (. ^Interval duration (getTotalSeconds)))))]]
     [:> react/Text {:style {:color "gray" :paddingLeft "2.5%"}}
                    goal]]))

(defn- paths
  [uid]
  (let [route (work/pull! [{:directions/steps [:step/geometry :step/duration :step/mode
                                               {:stop_times/trip [{:trip/route [:route/color :route/long_name]}]}]}]
                          [:directions/uuid uid])]
    (for [steps (partition-by :step/mode (:directions/steps @route))
          :let [coords (mapcat :coordinates (map :step/geometry steps))
                stroke (route-color (:trip/route (:stop_times/trip (first steps))))]]
      ^{:key (hash steps)}
      [:> expo/MapPolyline {:coordinates (map geometry/latlng coords)
                            :strokeColor stroke
                            :strokeWidth 4}])))

(defn Instructions
  "basic navigation directions.

   Async, some data might be missing when rendered !!"
  [props]
  (let [window (tool/keywordize (. fl/ReactNative (Dimensions.get "window")))
        uid   @(work/q! '[:find ?uid .
                          :where [_      :user/directions ?route]
                                 [?route :directions/uuid ?uid]])]
    [:> react/ScrollView {:flex 1}
      [:> react/View {:height (* 0.9 (:height window))}
        [symbols/CityMap
          (when (some? uid)
            (paths uid))]]
      [:> react/View {:flex 1 :backgroundColor "white"}
        (when (some? uid)
          [Info {:flex 1 :paddingTop "1%"} uid])
        (when (some? uid)
          [Route uid])]
      [:> react/View (merge (symbols/circle 52) symbols/shadow
                            {:position "absolute" :right "10%"
                             :top (* 0.88 (:height window))})
        [:> expo/Ionicons {:name "ios-navigate" :size 62 :color "blue"}]]]))

;hive.rework.state/conn
;(into {} (work/entity [:route/uuid #uuid"5b2d247b-f8c6-47f3-940e-dee71f97d451"]))
;(work/q queries/routes-ids)

;(let [id      (data/q queries/user-id (work/db))]
;  (:steps (:route/route (:user/route @(work/pull! [{:user/route [:route/route]}]
;                                                  [:user/id id])))))
