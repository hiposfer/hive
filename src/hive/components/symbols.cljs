(ns hive.components.symbols
  (:require [hive.components.foreigns.react :as react]
            [hive.libs.geometry :as geometry]
            [hive.components.foreigns.expo :as expo]))

(defn PointOfInterest
  "Components for displaying location related items. Usually used inside a List"
  [left-icon icon-text title subtitle right-icon]
  [:> react/View {:style {:flex-direction "row" :flex 1}}
    [:> react/View {:style {:flex 0.15 :alignItems "center" :justifyContent "flex-end"}}
      left-icon
      icon-text]
    [:> react/View {:style {:flex 0.7 :justifyContent "flex-end"}}
       title
       subtitle]
    [:> react/View {:style {:flex 0.1 :justifyContent "flex-end"}}
      (when (some? right-icon) right-icon)]])

(defn CityMap
  "a React Native MapView component which will only re-render on user-city change"
  [user]
  (let [coords (:coordinates (:city/geometry (:user/city user)))]
    [:> expo/MapView {:region (merge (geometry/latlng coords)
                                     {:latitudeDelta 0.02 :longitudeDelta 0.02})
                      :showsUserLocation     true :style {:flex 1}
                      :showsMyLocationButton true}]))

(def shadow
  {:elevation 3 :shadowColor "#000000" :shadowRadius 5
   :shadowOffset {:width 0 :height 3} :shadowOpacity 1.0})

(defn circle
  [radius]
  {:width radius :height radius :borderRadius (/ radius 2)
   :alignItems "center" :justifyContent "center"})
