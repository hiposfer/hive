(ns hive.screens.symbols
  (:require [react-native :as React]
            [expo :as Expo]
            [hive.state.core :as state]
            [hive.state.queries :as queries]
            [hive.utils.geometry :as geometry]))

(defn PointOfInterest
  "Components for displaying location related items. Usually used inside a List"
  [left-icon icon-text title subtitle right-icon]
  [:> React/View {:style {:flex-direction "row" :flex 1}}
    [:> React/View {:style {:flex 0.15 :alignItems "center" :justifyContent "flex-end"}}
      left-icon
      icon-text]
    [:> React/View {:style {:flex 0.7 :justifyContent "flex-end"}}
       title
       subtitle]
    [:> React/View {:style {:flex 0.1 :justifyContent "flex-end"}}
      (when (some? right-icon) right-icon)]])

(defn CityMap
  "a React Native MapView component which will only re-render on user-city change"
  [children]
  (let [bbox   @(state/q! queries/user-area-bbox)
        position @(state/q! queries/user-position)
        area (geometry/mapview-region {:children children
                                       :bbox     bbox
                                       :position position})]
    (if (nil? bbox)
      [:> React/View {:flex 1 :alignItems "center" :justifyContent "center"}
        [:> React/ActivityIndicator {:color "blue" :size "large"}]]
      [:> Expo/MapView {:region                area
                        :showsUserLocation     true
                        :style                 {:flex 1}
                        :showsMyLocationButton true}
                       children])))

(def shadow
  {:elevation 3 :shadowColor "#000000" :shadowRadius 5
   :shadowOffset {:width 0 :height 3} :shadowOpacity 1.0})

(defn circle
  [radius]
  {:width radius :height radius :borderRadius (/ radius 2)
   :alignItems "center" :justifyContent "center"})
