(ns hive.components.symbols
  (:require [hive.components.foreigns.react :as react]
            [hive.libs.geometry :as geometry]
            [hive.components.foreigns.expo :as expo]
            [hive.rework.core :as work]))

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
  [& content]
  (let [geometry @(work/q! '[:find ?geometry .
                             :where [?id :user/id]
                                    [?id :user/city ?city]
                                    [?city :city/geometry ?geometry]])
        region (merge (geometry/latlng (:coordinates geometry))
                      {:latitudeDelta 0.02 :longitudeDelta 0.02})]
    (if (nil? (:coordinates geometry))
      [:> expo/Ionicons {:name "ios-hammer" :size 26 :style {:flex 1 :top "50%" :left "50%"}}]
      (into [:> expo/MapView {:region region
                              :showsUserLocation true
                              :style {:flex 1}
                              :showsMyLocationButton true}]
            content))))

(def shadow
  {:elevation 3 :shadowColor "#000000" :shadowRadius 5
   :shadowOffset {:width 0 :height 3} :shadowOpacity 1.0})

(defn circle
  [radius]
  {:width radius :height radius :borderRadius (/ radius 2)
   :alignItems "center" :justifyContent "center"})
