(ns hive.components.elements
  (:require [cljs.core.async :refer-macros [go go-loop]]
            [hive.components.core :refer [View Button Icon Text ListItem ListBase
                                          Body Container Content Card CardItem Image]]
            [hive.rework.core :as rework]
            [hive.queries :as queries]
            [hive.foreigns :as fl]
            [hive.services.geocoding :as geocoding]
            [hive.rework.util :as tool]
            [cljs.core.async :as async]
            [clojure.string :as str]))

(defn move-to
  [user-id city-name]
  [{:user/id user-id
    :user/city [:city/name city-name]}])

(defn change-city!
  [props name]
  (rework/transact! queries/user-id move-to name)
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


(defn- update-places
  "transact the geocoding result under the user id"
  [id features]
  [{:user/id id
    :user/places features}])

(def autocomplete!
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  (rework/pipe geocoding/autocomplete!
               ;tool/log
               #(rework/transact! queries/user-id update-places (:features %))))

(defn places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [features]
  [:> ListBase
   (for [target features]
     ^{:key (:id target)}
     [:> ListItem ;{:on-press #(router/dispatch [:map/directions target :user/goal])}
      [:> Body
       [:> Text (:text target)]
       [:> Text {:note true :style {:color "gray"}}
                (str/join ", " (map :text (:context target)))]]])])

;(go (async/<! (autocomplete! {::geocoding/query "Cartagena, Colombia"
;                              ::geocoding/mode  "mapbox.places"]]])])

;(rework/transact! queries/user-db-index (fn [id] [[:db.fn/retractAttribute id :user/places]]))
