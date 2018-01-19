(ns hive.core
  (:require [reagent.core :as r]
            [hive.foreigns :as fl]
            [hive.components.core :refer [View Image Text TouchableHighlight Icon]]
            [hive.state :as state]
            [hive.services.location :as position]
            [hive.services.raw.location :as location]
            [hive.components.screens :as screens]
            [hive.components.navigation :as nav]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [datascript.core :as data]))

(defn root-ui []
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
  [] ;; todo: add https://github.com/reagent-project/historian
  (let [conn (data/create-conn state/schema)
        data (cons {:app/session (data/squuid)} state/defaults)]
    (data/transact! conn data)
    (work/init! conn)
    (go-try
      (let [remover (location/watch! position/defaults)]
        (work/transact! (<? remover)))
      (catch :default error
        (fl/toast! (ex-message error))))
    (.registerComponent fl/app-registry "main"
      #(r/reactify-component root-ui))))


;; this also works but it is not as clear
;(async/take! (location/watch! {::location/enableHighAccuracy true
;                               ::location/timeInterval 3000})
;             cljs.pprint/pprint)

;(:eavt @hive.rework.state/conn)
