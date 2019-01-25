(ns hive.screens.home.directions.trip-overview
  (:require [hive.screens.symbols :as symbols]
            [react-native :as React]
            [expo :as Expo]
            [goog.date.duration :as duration]
            [clojure.string :as str]
            [hive.assets :as assets]
            [hive.state.core :as state]
            [hive.utils.miscelaneous :as misc]
            [hive.state.queries :as queries]
            [datascript.core :as data]
            [hive.state.schema :as schema]
            [hiposfer.gtfs.edn :as gtfs]
            [hive.services.kamal :as kamal])
  (:import (goog.date DateTime)))

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

(defn TripIcon
  ([trip]
   (TripIcon trip {}))
  ([trip props]
   (case (:route/type (:trip/route trip))
     0 [:> assets/Ionicons (merge props {:name "ios-train" :size 32})]
     1 [:> assets/Ionicons (merge props {:name "ios-subway" :size 32})]
     2 [:> assets/Ionicons (merge props {:name "md-train" :size 32})]
     3 [:> assets/Ionicons (merge props {:name "ios-bus" :size 32})]
     ;; default
     [:> React/ActivityIndicator])))

(defn route-type-name
  [trip]
  (let [route-types (filter #(= :route/type (:keyword %))
                            (gtfs/fields schema/gtfs-data))]
    (first
      (for [entry (:values (first route-types))
            :when (= (:value entry) (:route/type (:trip/route trip)))]
        (first (str/split (:description entry) #"\.|,"))))))

(let [route-type (first (filter #(= :route/type (:keyword %))
                              (gtfs/fields schema/gtfs-data)))]
  (:values route-type))

(defn- StepOverviewMsg
  [props steps]
  (let [navigate (:navigate (:navigation props))]
    [:> React/TouchableOpacity {:style {:flex 1 :justifyContent "center"}
                                :onPress #(navigate "trip-overview")}
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

(def time-style {:textAlign "center" :color "gray" :fontSize 12})

(defn- RouteSectionTimes
  [props steps]
  [:> React/View {:width 40 :justifyContent "space-between"}
    [:> React/Text {:style time-style}
      (when (or (= "transit" (:step/mode (first steps)))
                (= "depart" (:maneuver/type (:step/maneuver (first steps)))))
        (misc/hour-minute (:step/arrive (first steps))))]
    (when (or (= "transit" (:step/mode (last steps)))
              (= "arrive" (:maneuver/type (:step/maneuver (last steps)))))
      [:> React/Text {:style time-style}
                     (misc/hour-minute (:step/arrive (last steps)))])])

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

(def frequency-trip '[:find ?frequency ?start-time
                      :in $ ?trip-id ?current-time
                      :where [?trip      :trip/id ?trip-id]
                             [?frequency :frequency/trip ?trip]
                             [?frequency :frequency/start_time ?start-time]
                             [?frequency :frequency/end_time ?end-time]
                             [(< ?start-time ?current-time)]
                             [(< ?current-time ?end-time)]])

(defn- seconds-of-day
  [^js/Date date]
  (+ (. date (getSeconds))
     (* 60 (+ (. date (getMinutes))
              (* 60 (. date (getHours)))))))

(defn Screen
  "basic navigation directions.

   Async, some data might be missing when rendered !!"
  [props]
  (let [trip-id (:trip/id (:params (:state (:navigation props))))
        secs    (seconds-of-day (new js/Date "2018-05-07T10:15:30+01:00"))
        ;window  (misc/keywordize (React/Dimensions.get "window"))
        ;_ (kamal/query! (state/db) frequency-trip trip-id secs)
        route   (state/q! queries/user-route)
        trip    @(state/pull! [{:trip/route [:route/type
                                             :route/long_name
                                             :route/short_name]}]
                              [:trip/id trip-id])]
    [:> React/ScrollView {:flex 1}
      [:> React/View {:height 60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> React/Text {:style {:color "white" :fontSize 20}}
                       "Trip Overview"]]
     [:> React/View {:flex 1 :backgroundColor "white" :padding 15}
       [:> React/View {:flex 1 :flex-direction "row"}
         [TripIcon trip]
         [:> React/View {:flex 1}
           [:> React/Text {:style {:paddingLeft 10 :color "slategray"}}
             (str (route-type-name trip) " "
                  (:route/short_name (:trip/route trip)))]
          [:> React/Text {:style {:paddingLeft 10 :color "slategray"}}
                         (:route/long_name (:trip/route trip))]]]
       (when (some? @route)
         [Route props @route])]]))
