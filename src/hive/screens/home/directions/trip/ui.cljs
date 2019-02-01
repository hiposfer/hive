(ns hive.screens.home.directions.trip.ui
  "The Trip Overview Screen is where the user can see details about
  the vehicle and its associated entities like Service provider, start
  and end time, service days and more. This is also where a User can decide
  to suggest a change to a Trip data"
  (:require [react-native :as React]
            [expo :as Expo]
            [clojure.string :as str]
            [hive.assets :as assets]
            [hive.state :as state]
            [hive.queries :as queries]
            [hive.utils.miscelaneous :as misc]
            [hive.screens.symbols :as symbols]
            [hive.screens.home.directions.trip.handlers :as handle])
  (:import (goog.date DateTime)))

(def big-circle 16)
(def section-height {"walking" 90 "transit" 120})

(defn TransitLine
  [steps]
  (let [stroke      (handle/route-color (:trip/route (:step/trip (first steps))))]
    [:> React/View {:width 20 :alignItems "center"}
      [:> React/View (merge {:backgroundColor stroke}
                            (symbols/circle big-circle))]
      [:> React/View {:backgroundColor stroke :width "15%" :flex 1}]
      [:> React/View (merge {:backgroundColor stroke :borderColor "transparent"}
                            (symbols/circle big-circle))]]))

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

(defn- StepOverviewMsg
  [steps]
  [:> React/View {:flex 1 :justifyContent "center"}
    [:> React/View {:flex-direction "row" :alignItems "center"}
      [:> React/View {:paddingRight 10 :with 32}
        [TripIcon (:step/trip (first steps))]]
      [:> React/Text {:style {:color "gray" :paddingRight 7 :flex 3}}
        (str/replace "";(:maneuver/instruction (:step/maneuver (first steps)))
                     "[Dummy]"
                     "")]]])

(defn- StepOverview
  [steps]
  [:> React/View {:flex 1 :justifyContent "space-between"}
    [:> React/Text {:style {:height 20}}
                   (some :step/name (butlast steps))]
    [StepOverviewMsg steps]
    [:> React/Text {:style {:height 20}}
                   (:step/name (last steps))]])

(def time-style {:textAlign "center" :color "gray" :fontSize 12})

(defn- RouteSectionTimes
  [steps]
  [:> React/View {:width 40 :justifyContent "space-between"}
    [:> React/Text {:style time-style}
                   (handle/hour-minute (:step/arrive (first steps)))]
    [:> React/Text {:style time-style}
                   (handle/hour-minute (:step/arrive (last steps)))]])

(defn- RouteSection
  [steps]
  (let [height     (get section-height "transit")]
    [:> React/View {:height height :flexDirection "row"}
      [RouteSectionTimes steps]
      [TransitLine steps]
      [StepOverview steps]]))

(defn- Route
  [props user-route]
  (let [trip-id (:trip/id (:params (:state (:navigation props))))
        route   @(state/pull! [{:directions/steps
                                [:step/arrive
                                 :step/mode
                                 :step/name
                                 {:step/maneuver [:maneuver/type]}
                                 {:step/trip [:trip/id
                                              {:trip/route [:route/long_name
                                                            :route/short_name
                                                            :route/type
                                                            :route/color]}]}]}]
                              user-route)]
    [:> React/View {:flex 1}
      (for [steps (partition-by :step/mode (:directions/steps route))
            :when (= trip-id (:trip/id (:step/trip (first steps))))]
        ^{:key (:step/arrive (first steps))}
        [RouteSection steps])]))


(defn Screen
  [props]
  (let [trip-id   (:trip/id (:params (:state (:navigation props))))
        route     (state/q! queries/user-route)
        datoms    @(state/q! queries/frequency-trip
                             trip-id
                             (handle/seconds-of-day (new js/Date "2018-05-07T10:15:30+01:00")))
        frequency (handle/datoms->map datoms {})
        trip      @(state/pull! [:trip/headsign
                                 {:trip/service [:service/monday
                                                 :service/tuesday
                                                 :service/wednesday
                                                 :service/thursday
                                                 :service/friday
                                                 :service/saturday
                                                 :service/sunday]}
                                 ;:trip/service
                                 {:trip/route [:route/type
                                               :route/short_name]}]
                                [:trip/id trip-id])]
    [:> React/ScrollView {:flex 1 :backgroundColor "white"}
      [:> React/View {:height 60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> React/Text {:style {:color "white" :fontSize 20}}
                       "Trip Overview"]]
     [:> React/View {:flex 1 :padding 15}
       [:> React/View {:flex 1 :flex-direction "row" :paddingBottom 20}
         [TripIcon trip]
         [:> React/View {:flex 1}
           [:> React/Text {:style {:paddingLeft 10 :color "slategray"}}
                          (handle/route-type-name trip)]
          [:> React/Text {:style {:paddingLeft 10 :color "slategray"}}
                         (str "direction: "
                              (str/replace (:trip/headsign trip)
                                           "[Dummy]"
                                           ""))]]]
       [:> React/View {:flex 1}
         [:> React/Text {:style {:color "slategray"}}
           (str "Every " (misc/convert (:frequency/headway_secs frequency)
                                       :from "seconds" :to "minutes")
                " minutes from "
                (handle/time-since-midnight (:frequency/start_time frequency))
                " to "
                (handle/time-since-midnight (:frequency/end_time frequency)))]
         [:> React/Text {:style {:paddingLeft 10 :color "slategray"}}
                        (if (or (= 0 (:frequency/exact_times frequency))
                                (nil? (:frequency/exact_times frequency)))
                          "NOT EXACT"
                          "EXACT")]]
       (when (some? @route)
         [Route props @route])]]))
