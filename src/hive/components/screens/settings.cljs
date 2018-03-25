(ns hive.components.screens.settings
  (:require [hive.components.native-base :as base]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [hive.services.store :as store]
            [hive.queries :as queries]
            [hive.components.navigation :as nav]
            [reagent.core :as r]))

(defn move-to!
  [city user goBack]
  (let [tx {:user/id user
            :user/city [:city/name (:city/name city)]}]
    (work/transact! [tx])
    (store/save! (select-keys tx [:user/city]))
    (goBack)))

(defn city-selector
  [props]
  (let [city (:city props)
        user (:user props)
        goBack (:goBack (:navigation props))]
    [:> base/ListItem {:on-press #(move-to! city user goBack)}
     [:> base/Body {}
      [:> base/Text (:city/name city)]
      [:> base/Text {:note true :style {:color "gray"}}
                    (str (:city/region city) ", " (:city/country city))]]]))

(defn settings
  [props]
  (let [cities  @(work/q! queries/cities)
        user     (work/q queries/user-id)
        navigate (:navigate (:navigation props))]
    [:> base/Container
     [:> base/Header
      [:> base/Button {:transparent true :full true
                       :on-press #(navigate "DrawerToggle")}
       [:> base/Icon {:name "menu"}]]
      [:> base/Body [:> base/Title "Settings"]]]
     [:> base/Content
        (for [city cities]
          ^{:key (:city/name city)}
          [city-selector (assoc props :city city :user user)])]]))

(def Screen (nav/drawer-screen settings
              {:title      "Settings"
               :drawerIcon (r/as-element [:> base/Icon {:name "settings"}])}))
