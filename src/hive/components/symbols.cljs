(ns hive.components.symbols
  (:require [hive.components.foreigns.react :as react]
            [hive.components.foreigns.expo :as expo]
            [hive.components.foreigns.native-base :as base]))

(defn point-of-interest
  [{:keys [left-icon icon-text title subtitle right-icon]}]
  [:> react/View {:style {:flex-direction "row" :flex 1}}
   [:> react/View {:style {:flex 0.15 :alignItems "center" :justifyContent "flex-end"}}
    [:> expo/Ionicons (if (string? left-icon) {:name left-icon} left-icon)]
    [:> react/Text {:note true :style (merge {:color "gray"} (:style icon-text))}
                  (or (:content icon-text) icon-text)]]
   [:> react/View {:style {:flex 0.7 :justifyContent "flex-end"}}
    [:> react/Text (select-keys title [:style])
                  (or (:content title) title)]
    [:> react/Text {:note true :style (merge {:color "gray"} (:style subtitle))}
                  (or (:content subtitle) subtitle)]]
   [:> react/View {:style {:flex 0.1 :justifyContent "flex-end"}}
    (when right-icon
      [:> expo/Ionicons (if (string? right-icon) {:name right-icon} right-icon)])]])
