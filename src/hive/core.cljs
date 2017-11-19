(ns hive.core
  (:require [reagent.core :as r]
            [posh.reagent :as posh]
            [com.stuartsierra.component :as component]
            [datascript.core :as data]
            [cljs-react-navigation.reagent :as nav]
            [hive.foreigns :as fl]
            [hive.state :as state]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def Text (.-Text ReactNative))
(def View (.-View ReactNative))
(def Image (.-Image ReactNative))
(def TouchableHighlight (.-TouchableHighlight ReactNative))
(def Alert (.-Alert ReactNative))

(defonce ReactNavigation (js/require "react-navigation"))

(defn alert [title]
  (.alert Alert title))

(defn home
  [{:keys [conn] :as system}]
  (let [name (posh/q '[:find ?name . :in $ ?age :where [?e :user/age ?age]
                                                       [?e :user/name ?name]]
                      conn 27)]
    (fn [{:keys [screenProps navigation] :as props}]
      (let [{:keys [navigate goBack]} navigation]
        [:> View {:style {:flex-direction "column" :margin 40 :align-items "center"}}
         [:> Image {:source (js/require "./assets/images/cljs.png")
                    :style  {:width 200
                             :height 200}}]
         [:> Text {:style {:font-size  30 :font-weight "100" :margin-bottom 20
                           :text-align "center"}}
          "Hello World"]
         [:> TouchableHighlight {:style {:background-color "#999" :padding 10
                                         :border-radius 5}
                                 :on-press #(navigate "Settings")}
          [:> Text {:style {:color "white" :text-align "center"
                            :font-weight "bold"}}
           "press me"]]]))))

(defn settings
  [{:keys [screenProps navigation] :as props}]
  (let [{:keys [navigate goBack]} navigation]
    [:> TouchableHighlight {:style {:background-color "#999" :padding 10
                                    :border-radius 5}
                            :on-press #(goBack)}
     [:> Text {:style {:color "white" :text-align "center"
                       :font-weight "bold"}}
      "Go Home"]]))

(defn root-ui
  [system]
  (let [HomeScreen     (nav/stack-screen (home system) {:title "Home"})
        SettingsScreen (nav/stack-screen settings {:title "Settings"})
        HomeStack      (nav/stack-navigator {:Home {:screen HomeScreen}
                                             :Settings {:screen SettingsScreen}})]
    (fn [] [:> HomeStack])))

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
      (assoc this :init (posh/posh! conn)
        this)))
  (stop [this] (assoc this :registry nil)))
;; ----------------------------------
;; pass the necessary components around to create
;; all the necessary bindings
(defrecord UiContainer [conn posh state ui]
  component/Lifecycle
  (start [this]
    (if (nil? ui)
      (assoc this :ui (root-ui this))
      this))
  (stop [this] (assoc this :ui nil)))
;; ------------------------------
;; encapsulates the React Native registry
;; dont start before state was created
(defrecord RnRegistry [root-ui registry]
  component/Lifecycle
  (start [this]
    (if (nil? registry)
      (assoc this :registry (.registerComponent app-registry
                              "main"
                              #(r/reactify-component (:ui root-ui))))
      this))
  (stop [this]
    (.unmountApplicationComponentAtRootTag app-registry "main")
    (assoc this :registry nil)))

(defn system
  []
  (component/system-map
    :conn      (data/create-conn state/schema)
    :state     (component/using (map->StateInitializer {})
                                [:conn])
    :posh      (component/using (map->PoshContainer {})
                                [:conn])
    :root-ui   (component/using (map->UiContainer {})
                                [:conn :state :posh])
    :registry  (component/using (map->RnRegistry {})
                                [:root-ui])))

;conn

;(data/q '[:find ?age
;          :in $ ?name
;          :where [?e :user/name ?name]
;                 [?e :user/age ?age]]
;        @conn "alice")

;(data/pull @conn [:user/name :user/age] [:user/id "1"])

;(posh/pull conn [:user/name :user/age] [:user/id "1"])

;(posh/transact! (:conn @env.main/app)
;                [{:user/id "1"
;                  :user/name "hello world"
;                  :user/age 27}])