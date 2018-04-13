(ns hive.components.symbols
  (:require [hive.components.react :as react]
            [hive.components.native-base :as base]))

(defn point-of-interest
  [{:keys [left-icon icon-text title subtitle right-icon]}]
  [:> react/View {:style {:flex-direction "row" :flex 1}}
   [:> react/View {:style {:flex 0.15 :alignItems "center" :justifyContent "flex-end"}}
    [:> base/Icon (if (string? left-icon) {:name left-icon} left-icon)]
    [:> base/Text {:note true :style (merge {:color "gray"} (:style icon-text))}
                  (or (:content icon-text) icon-text)]]
   [:> react/View {:style {:flex 0.7 :justifyContent "flex-end"}}
    [:> base/Text (select-keys title [:style])
                  (or (:content title) title)]
    [:> base/Text {:note true :style (merge {:color "gray"} (:style subtitle))}
                  (or (:content subtitle) subtitle)]]
   [:> react/View {:style {:flex 0.1 :justifyContent "flex-end"}}
    (when right-icon
      [:> base/Icon (if (string? right-icon) {:name right-icon} right-icon)])]])
