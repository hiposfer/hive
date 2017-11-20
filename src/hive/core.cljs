(ns hive.core
  (:require [reagent.core :as r]
            [com.stuartsierra.component :as component]
            [posh.reagent :as posh]
            [posh.plugin-base :as pbase]
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
                                             :Settings {:screen SettingsScreen}}
                                            {:headerMode "none"})]
    [:> HomeStack]))

;; ---------------------------------
;; populates the DataScript in-memory database
(defrecord StateInitializer [conn init]
  component/Lifecycle
  (start [this]
    (if (nil? init)
      (assoc this :init (data/transact! conn state/defaults))
      this))
  (stop [this] (assoc this :init nil)))
;; ---------------------------------
;; encapsulates Posh initialization
(defrecord PoshContainer [conn init]
  component/Lifecycle
  (start [this]
    (if (nil? init)
      (assoc this :init (pbase/posh! posh/dcfg conn)
        this)))
  (stop [this] (assoc this :registry nil)))
;; ----------------------------------
;; encapsulates the React Native registry
;; dont start before state was created
(defrecord RnRegistry [conn state posh registry]
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
    :conn      (data/create-conn state/schema)
    :state     (component/using (map->StateInitializer {})
                                [:conn])
    :posh      (component/using (map->PoshContainer {})
                                [:conn])
    :registry  (component/using (map->RnRegistry {})
                                [:conn :state :posh])))