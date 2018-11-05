(ns hive.screens.home.gtfs
  (:require [react-native :as React]
            [datascript.core :as data]
            [clojure.string :as str]
            [hiposfer.geojson.specs :as geojson]
            [clojure.spec.alpha :as s]
            [hive.screens.symbols :as symbols]
            [expo :as Expo]
            [hive.state.core :as state]))

;; taken from https://stackoverflow.com/a/44357409
(defn Table
  [rows]
  [:> React/View {:style {:flex 1 :alignItems "center" :justifyContent "center"}}
   (for [row rows]
     ^{:key (hash row)}
      [:> React/View {:flex 1 :alignSelf "stretch" :flexDirection "row"
                      :paddingLeft 20 :paddingTop 20}
        (for [cell row]
          ^{:key (hash cell)}
           [:> React/View {:style {:flex 1 :alignSelf "stretch"}}
             [:> React/Text (str cell)]])])])

(defn Data
  [props]
  (let [ref    (first (:params (:state (:navigation props))))
        data   (into {} (data/entity (state/db) ref))
        points (for [[k v] data :when (s/valid? ::geojson/point v)] [k v])]
    [:> React/View {:flex 1}
      ;; Header .....................
      [:> React/View {:height          60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> React/Text {:style {:color "white" :fontSize 20}}
                       (str/capitalize (namespace (key ref)))]]
      ;; Data Table .....................
      (when (not-empty points)
        [:> React/View {:height 400}
          [symbols/CityMap
            (for [[k v] points
                  :let [coords (:coordinates v)]]
              ^{:key (hash coords)}
              [:> Expo/MapView.Marker {:coordinate {:latitude (second coords)
                                                    :longitude (first coords)}}])]])
      [:> React/View {:height 200}
        [Table (for [row data] (for [v row] (if (keyword? v) (name v) v)))]]]))
