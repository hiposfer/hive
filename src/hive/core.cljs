(ns hive.core
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [hive.rework.reagent.tx :as rtx]
            [datascript.core :as data]
            [hive.foreigns :as fl]
            [hive.components.core :refer [View Image Text TouchableHighlight Icon]]
            [hive.state :as state]
            [hive.components.screens :as screens]
            [hive.components.navigation :as nav]
            [hive.services.http :as http]))

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

;; ---------------------------------
;; populates the DataScript in-memory database
(defrecord StateContainer [schema conn ratom]
  component/Lifecycle
  (start [this]
    (if-not (nil? conn) this
      (let [conn (data/create-conn schema)]
        (data/transact! conn state/defaults)
        (assoc this :conn conn
                    :ratom (rtx/listen! conn)))))
  (stop [this] (assoc this :conn nil :ratom nil)))
;; ----------------------------------
;; encapsulates the React Native registry
;; dont start before state was created
(defrecord RnRegistry [state registry]
  component/Lifecycle
  (start [this]
    (if (nil? registry)
      (assoc this :registry
        (.registerComponent fl/app-registry
          "main"
          #(r/reactify-component root-ui)))
      this))
  (stop [this]
    (.unmountApplicationComponentAtRootTag fl/app-registry "main")
    (assoc this :registry nil)))

(defn system
  []
  (component/system-map
    :schema    state/schema
    :state     (component/using (map->StateContainer {})
                                [:schema])
    ::http/service (http/->Service nil)
    :registry  (component/using (map->RnRegistry {})
                                [:state ::http/service])))