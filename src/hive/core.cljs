(ns hive.core
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [hive.rework.reagent.tx :as rtx]
            [datascript.core :as data]
            [cljs-react-navigation.reagent :as nav]
            [hive.foreigns :as fl]
            [hive.components.core :refer [View Image Text TouchableHighlight]]
            [hive.state :as state]
            [hive.components.screens :as screens]))

;; TODO: this should be a DrawerNavigation but it hasnt been wrapped yet in
;; https://github.com/seantempesta/cljs-react-navigation/issues/3
(defn root-ui
  []
  (let [HomeScreen     (nav/stack-screen screens/home {:title "Home"})
        SettingsScreen (nav/stack-screen screens/settings {:title "Settings"})
        HomeStack      (nav/stack-navigator {:Home {:screen HomeScreen}
                                             :Settings {:screen SettingsScreen}})]
                                            ;{:headerMode "none"})]
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
    :registry  (component/using (map->RnRegistry {})
                                [:state])))