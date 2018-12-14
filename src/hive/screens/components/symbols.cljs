(ns hive.screens.components.symbols
  (:require [react-native :as React]
            [expo :as Expo]
            [hiposfer.geojson.specs :as geojson]
            [hive.state.core :as state]))

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

(defn- region
  [children default-center]
  (let [lines  (for [child (tree-seq coll? seq children)
                     :when (and (map? child) (contains? child :coordinates))
                     :let [coords (:coordinates child)]]
                 (linestring coords))
        points (for [child (tree-seq coll? seq children)
                     :when (and (map? child) (contains? child :coordinate))
                     :let [coords (:coordinate child)]]
                 (point coords))
        coll   {:type       "GeometryCollection"
                :geometries (concat lines points [default-center])}
        [minx, miny, maxx, maxy] (geojson/bbox coll)]
    (if (= 1 (count (:geometries coll))) ;; more than just the default
      {:latitudeDelta 0.02 :longitudeDelta 0.02
       :latitude (second (:coordinates default-center))
       :longitude (first (:coordinates default-center))}
      {:latitude (/ (+ miny maxy) 2)
       :longitude (/ (+ maxx minx) 2)
       :latitudeDelta (Math/abs (- miny maxy))
       :longitudeDelta (+ (Math/abs (- maxx minx)) space)})))

(defn CityMap
  "a React Native MapView component which will only re-render on user-city change"
  [children]
  (let [bbox   @(state/q! '[:find ?bbox .
                            :where [?id :user/uid]
                                   [?id :user/area ?area]
                                   [?area :area/bbox ?bbox]])
        [minx, miny, maxx, maxy] bbox
        center (point {:latitude  (/ (+ miny maxy) 2)
                       :longitude (/ (+ maxx minx) 2)})
        area   (region children center)]
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
