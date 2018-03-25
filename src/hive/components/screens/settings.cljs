(ns hive.components.screens.settings
  (:require [hive.components.native-base :as base]
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
            ((:goBack (:navigation props)))
            (catch :default error (cljs.pprint/pprint error)))))

(defn city-selector
  [city props]
  ^{:key (:city/name city)}
  [:> base/ListItem {:on-press #(move-to! city props)}
   [:> base/Body {}
    [:> base/Text (:city/name city)]
    [:> base/Text {:note true :style {:color "gray"}}
                  (str (:city/region city) ", " (:city/country city))]]])

(defn settings
  [props]
  (let [cities @(work/q! queries/cities)
        navigate (:navigate (:navigation props))]
    [:> base/Container
     [:> base/Header
      [:> base/Button {:transparent true :full true
                       :on-press #(navigate "DrawerToggle")}
       [:> base/Icon {:name "menu"}]]
      [:> base/Body [:> base/Title "Settings"]]]
     [:> base/Content
        (map city-selector cities (repeat props))]]))

(def Screen (nav/drawer-screen settings
              {:title      "Settings"
               :drawerIcon (r/as-element [:> base/Icon {:name "settings"}])}))
