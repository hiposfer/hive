(ns hive.core
  (:require [reagent.core :as r]
            [hive.foreigns :as fl]
            [hive.components.core :refer [View Image Text TouchableHighlight Icon]]
            [hive.state :as state]
            [hive.services.location :as location]
            [hive.components.screens :as screens]
            [hive.components.navigation :as nav]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.rework.core :as rework]
            [datascript.core :as data]))

(defn root-ui
  []
  (let [HomeDirections (rn-nav/stack-screen screens/directions
                         {:title "directions"})
        HomeMap        (rn-nav/stack-screen screens/home
                         {:title "map"})
        HomeStack      (rn-nav/stack-navigator
                         {:map        {:screen HomeMap}
                          :directions {:screen HomeDirections}}
                         {:headerMode "none"})
        Home     (nav/drawer-screen HomeStack
                   {:title "Home"
                    :drawerIcon (r/as-element [:> Icon {:name "home"}])})
        Settings (nav/drawer-screen screens/settings
                   {:title "Settings"
                    :drawerIcon (r/as-element [:> Icon {:name "settings"}])})
        Root     (nav/drawer-navigator {:Home {:screen Home}
                                        :Settings {:screen Settings}}
                                       {})]
    [:> Root]))

(defn init!
  "register the main UI component in React Native"
  []
  (rework/init! state/schema state/defaults)
  (rework/transact! [{:app/session (data/squuid)}])
  (location/watch! {::location/enableHighAccuracy true
                    ::location/timeInterval 3000})
  (.registerComponent fl/app-registry "main"
                      #(r/reactify-component root-ui)))
