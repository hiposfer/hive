(ns hive.core
  (:require [reagent.core :as r]
            [hive.foreigns :as fl]
            [hive.components.core :refer [View Image Text TouchableHighlight Icon]]
            [hive.state :as state]
            [hive.components.screens :as screens]
            [hive.components.navigation :as nav]
            [hive.rework.core :as rework]))

(defn root-ui
  []
  (let [HomeScreen     (nav/drawer-screen screens/home
                         {:title "Home"
                          :drawerIcon (r/as-element [:> Icon {:name "home"}])})
        SettingsScreen (nav/drawer-screen screens/settings
                         {:title "Settings"
                          :drawerIcon (r/as-element [:> Icon {:name "settings"}])})
        HomeStack      (nav/drawer-navigator {:Home {:screen HomeScreen}
                                              :Settings {:screen SettingsScreen}}
                                             {})]
    [:> HomeStack]))

(defn init!
  "register the main UI component in React Native"
  []
  (rework/init! state/schema state/defaults)
  (.registerComponent fl/app-registry "main"
                      #(r/reactify-component root-ui)))
