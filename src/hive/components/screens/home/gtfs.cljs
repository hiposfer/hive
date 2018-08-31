(ns hive.components.screens.home.gtfs
  (:require [hive.components.foreigns.react :as react]
            [datascript.core :as data]
            [hive.rework.core :as work]
            [clojure.string :as str]))

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
  (let [ref  (first (:params (:state (:navigation props))))
        data (into {} (data/entity (work/db) ref))]
    [:> react/View {:flex 1}
      ;; Header .....................
      [:> react/View {:height          60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> react/Text {:style {:color "white" :fontSize 20}}
                       (str/capitalize (namespace (key ref)))]]
      ;; Data Table .....................
      [:> react/View {:height 200}
        [Table (for [row data] (for [v row] (if (keyword? v) (name v) v)))]]]))
