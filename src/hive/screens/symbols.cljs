(ns hive.screens.symbols
  (:require [react-native :as React]
            [hive.utils.geometry :as geometry]
            [expo :as Expo]
            [hive.assets :as assets]
            [hive.rework.core :as work]
            [hiposfer.geojson.specs :as geojson]))

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

(def coordinate (juxt :longitude :latitude))

(defn- linestring
  [obj]
  {:type "LineString"
   :coordinates (map coordinate obj)})

(defn- point
  [obj]
  {:type "Point"
   :coordinates (coordinate obj)})

(def space 0.005)

;; TODO: take into account the user position
(defn- region
  [children default-center]
  (let [lines (eduction (filter map?)
                        (filter :coordinates)
                        (map :coordinates)
                        (map linestring)
                        (tree-seq coll? seq children))
        points (eduction (filter map?)
                         (filter :coordinate)
                         (map :coordinate)
                         (map point)
                         (tree-seq coll? seq children))
        coll {:type "GeometryCollection"
              :geometries (concat lines points [(point default-center)])}
        [minx, miny, maxx, maxy] (geojson/bbox coll)]
    (if (= 1 (count (:geometries coll))) ;; more than just the default
      (merge {:latitudeDelta 0.02 :longitudeDelta 0.02} default-center)
      {:latitude (/ (+ miny maxy) 2)
       :longitude (/ (+ maxx minx) 2)
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
      [:> assets/Ionicons {:name "ios-hammer" :size 26 :style {:flex 1 :top "50%" :left "50%"}}]
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
