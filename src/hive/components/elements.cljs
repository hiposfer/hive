(ns hive.components.elements
  (:require [hive.components.core :refer [View Button Icon Text ListItem Body
                                          Container Content Card CardItem Image]]
            [hive.rework.core :as rework]
            [hive.queries :as queries]
            [hive.effects :as fx]
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


(defn change-city!
  [props name]
  (rework/transact! queries/user-id fx/move-to name)
  ((:navigate (:navigation props)) "Home"))

(defn city-selector
  [{:keys [city/name region country]} props]
  ^{:key name}
   [:> ListItem {:on-press #(change-city! props name)}
     [:> Body {}
       [:> Text name]
       [:> Text {:note true :style {:color "gray"}}
         (str region ", " country)]]])

(defn no-internet
  "display a nice little monster asking for internet connection"
  []
  (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card {:style {:width (* (:width dims) 0.95)}}
       [:> CardItem {:cardBody true}
        [:> Image {:style  {:width (* (:width dims) 0.9)
                            :height (* (:height dims) 0.8)
                            :resizeMode "contain" :flex 1}
                   :source fl/thumb-sign}]]]]]))

(defn user-location-error
  []
  (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card
       [:> CardItem {:cardBody true}
        [:> Image {:style {:width (* (:width dims) 0.9)
                           :height (* (:height dims) 0.7)
                           :resizeMode "contain" :flex 1}
                   :source fl/thumb-run}]]
       [:> CardItem
        [:> Body
         [:> Text "ERROR: we couldn't find your current position. This might be due to:"]
         [:> Text {:style {:textAlign "left"}} "\u2022 no gps connection enabled"]
         [:> Text "\u2022 bad signal reception"]]]]]]))
