(ns hive.components.elements
  (:require [hive.components.core :refer [View Button Icon Text ListItem Body]]
            [hive.rework.core :as rework]
            [hive.queries :as queries]
            [hive.effects :as fx]))


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


(defn change-city!
  [props name]
  (rework/transact! queries/user-id fx/move-to name)
  ((:navigate (:navigation props)) "Home"))

(defn city-selector
  [[name _ _ region country :as city] props]
  ^{:key name}
   [:> ListItem {:on-press #(change-city! props name)}
     [:> Body {}
       [:> Text name]
       [:> Text {:note true :style {:color "gray"}}
         (str region ", " country)]]])
