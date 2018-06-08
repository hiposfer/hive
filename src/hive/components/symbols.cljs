(ns hive.components.symbols
  (:require [hive.components.foreigns.react :as react]))

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
