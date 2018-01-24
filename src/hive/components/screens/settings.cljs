(ns hive.components.screens.settings
  (:require [hive.components.core :refer [View Button Icon Text ListItem ListBase
                                          Body Container Content Card CardItem Image
                                          Header Item Input Title]]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [hive.services.store :as store]
            [hive.queries :as queries]
            [hive.components.navigation :as nav]
            [reagent.core :as r]))

(defn update-city
  [data]
  {:user/id (:user/id data)
   :user/city [:city/name (:city/name data)]})

(defn move-to!
  [city props]
  (let [data (work/inject city :user/id queries/user-id)
        tx   (update-city data)]
    (go-try (work/transact! [tx])
            (<? (store/save! (select-keys tx [:user/city])))
            ((:navigate (:navigation props)) "Home")
            (catch :default error (cljs.pprint/pprint error)))))

(defn city-selector
  [city props]
  ^{:key (:city/name city)}
  [:> ListItem {:on-press #(move-to! city props)}
   [:> Body {}
    [:> Text (:city/name city)]
    [:> Text {:note true :style {:color "gray"}}
     (str (:city/region city) ", " (:city/country city))]]])

(defn settings
  [props]
  (let [cities @(work/q! queries/cities)
        navigate (:navigate (:navigation props))]
    [:> Container
     [:> Header
      [:> Button {:transparent true :full true
                  :on-press #(navigate "DrawerToggle")}
       [:> Icon {:name "menu"}]]
      [:> Body [:> Title "Settings"]]]
     [:> Content
      (map city-selector cities (repeat props))]]))

(def Screen (nav/drawer-screen settings
              {:title      "Settings"
               :drawerIcon (r/as-element [:> Icon {:name "settings"}])}))
