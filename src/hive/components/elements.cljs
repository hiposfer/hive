(ns hive.components.elements
  (:require [hive.components.core :refer [View Button Icon Text ListItem
                                          Body]]
            [hive.foreigns :as fl]))


;(defn targets-list
;  "list of items resulting from a geocode search, displayed to the user to choose his
;  destination"
;  [features]
;  [list-base
;   (for [target features]
;     ^{:key (:id target)}
;     [list-item ;{:on-press #(router/dispatch [:map/directions target :user/goal])}
;      [body
;       [text (:title target)]
;       [text {:note true :style {:color "gray"}} (:subtitle target)]]])])


(defn drawer-menu
  [{:keys [navigation] :as props}]
  (let [{:keys [navigate goBack]} navigation]
    [:> View {:activeOpacity 1}
     [:> Button {:full true} ;:on-press go-home}
      [:> Icon {:name "home"}]
      [:> Text {} "Home"]]
     [:> Button {:full true :on-press #(navigate "Settings")}
      [:> Icon {:name "settings"}]
      [:> Text {} "Settings"]]]))

(defn city-selector
  [cities]
  (for [city cities]
    [:> ListItem {}
      [:> Body {}
        [:> Text (:name city)]
        [:> Text {:note true :style {:color "gray"}}
                 (str (:region city) ", " (:country city))]]]))
