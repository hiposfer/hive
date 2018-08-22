(ns hive.components.symbols
  (:require [hive.components.foreigns.react :as react]
            [hive.libs.geometry :as geometry]
            [hive.components.foreigns.expo :as expo]
            [hive.rework.core :as work]
            [hiposfer.geojson.specs :as geojson]))

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

(def coordinate (juxt :longitude :latitude))

(defn- linestring
  [obj]
  {:type "LineString"
   :coordinates (map coordinate (:coordinates obj))})

(def space 0.005)

(defn- region
  [children default-center]
  (let [lines (eduction (filter map?)
                        (filter :coordinates)
                        (map linestring)
                        (tree-seq coll? seq children))
        coll  (assoc {:type "GeometryCollection"} :geometries lines)
        [minx, miny, maxx, maxy]  (geojson/bbox coll)]
    (if (empty? (:geometries coll))
      (merge {:latitudeDelta 0.02 :longitudeDelta 0.02} default-center)
      {:latitude (/ (+ miny maxy) 2) :longitude (/ (+ maxx minx) 2)
       :latitudeDelta (Math/abs (- miny maxy))
       :longitudeDelta (+ (Math/abs (- maxx minx)) space)})))

(defn CityMap
  "a React Native MapView component which will only re-render on user-city change"
  [children]
  (let [geometry @(work/q! '[:find ?geometry .
                             :where [?id :user/uid]
                                    [?id :user/city ?city]
                                    [?city :city/geometry ?geometry]])
        area      (region children (geometry/latlng (:coordinates geometry)))]
    (if (nil? (:coordinates geometry))
      [:> expo/Ionicons {:name "ios-hammer" :size 26 :style {:flex 1 :top "50%" :left "50%"}}]
      [:> expo/MapView {:region                area
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
