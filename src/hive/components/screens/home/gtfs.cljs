(ns hive.components.screens.home.gtfs
  (:require [hive.components.foreigns.react :as react]
            [datascript.core :as data]
            [hive.rework.core :as work]
            [clojure.string :as str]
            [hiposfer.geojson.specs :as geojson]
            [clojure.spec.alpha :as s]
            [hive.components.symbols :as symbols]
            [hive.components.foreigns.expo :as expo]))

;; taken from https://stackoverflow.com/a/44357409
(defn Table
  [rows]
  [:> react/View {:style {:flex 1 :alignItems "center" :justifyContent "center"}}
   (for [row rows]
     ^{:key (hash row)}
      [:> react/View {:flex 1 :alignSelf "stretch" :flexDirection "row"
                      :paddingLeft 20 :paddingTop 20}
        (for [cell row]
          ^{:key (hash cell)}
           [:> react/View {:style {:flex 1 :alignSelf "stretch"}}
             [:> react/Text (str cell)]])])])

(defn Data
  [props]
  (let [ref    (first (:params (:state (:navigation props))))
        data   (into {} (data/entity (work/db) ref))
        points (for [[k v] data :when (s/valid? ::geojson/point v)] [k v])]
    [:> react/View {:flex 1}
      ;; Header .....................
      [:> react/View {:height          60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> react/Text {:style {:color "white" :fontSize 20}}
                       (str/capitalize (namespace (key ref)))]]
      ;; Data Table .....................
      (when (not-empty points)
        [:> react/View {:height 400}
          [symbols/CityMap
            (for [[k v] points
                  :let [coords (:coordinates v)]]
              ^{:key (hash coords)}
              [:> expo/MapMarker {:coordinate {:latitude (second coords) :longitude (first coords)}}])]])
      [:> react/View {:height 200}
        [Table (for [row data] (for [v row] (if (keyword? v) (name v) v)))]]]))
