(ns hive.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [hive.handlers]
            [posh.reagent :as posh]
            [hive.subs]
            [datascript.core :as data]
            [cljs-react-navigation.reagent :as nav]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def Text (.-Text ReactNative))
(def View (.-View ReactNative))
(def Image (.-Image ReactNative))
(def TouchableHighlight (.-TouchableHighlight ReactNative))
(def Alert (.-Alert ReactNative))

(defonce ReactNavigation (js/require "react-navigation"))

(defonce schema {:user/id {:db.unique :db.unique/identity}
                 :user/name {}
                 :user/age {}
                 :user/parent {:db.valueType :db.type/ref
                               :db.cardinality :db.cardinality/many}})

(defonce conn (data/create-conn schema))
;conn

(defonce trans
  (data/transact! conn
                  [{:user/id "1"
                    :user/name "alice"
                    :user/age 27}
                   {:user/id "2"
                    :user/name "bob"
                    :user/age 29}
                   {:user/id "3"
                    :user/name "kim"
                    :user/age 2
                    :user/parent [[:user/id "1"]
                                  [:user/id "2"]]}
                   {:user/id "4"
                    :user/name "aaron"
                    :user/age 61}
                   {:user/id "5"
                    :user/name "john"
                    :user/age 39
                    :user/parent [[:user/id "4"]]}
                   {:user/id "6"
                    :user/name "mark"
                    :user/age 34}
                   {:user/id "7"
                    :user/name "kris"
                    :user/age 8
                    :user/parent [[:user/id "4"]
                                  [:user/id "5"]]}]))

(defn alert [title]
  (.alert Alert title))

(defn home
  []
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
          @name] ;"Hello World"]
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
      "Go Back"]]))

(defn app-root []
  (let [HomeScreen     (nav/stack-screen (home) {:title "Home"})
        SettingsScreen (nav/stack-screen settings {:title "Settings"})
        HomeStack      (nav/stack-navigator {:Home {:screen HomeScreen}
                                             :Settings {:screen SettingsScreen}})]
    [:> HomeStack]))

(defn init []
  (posh/posh! conn)
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "main" #(r/reactify-component app-root)))

;conn

;(data/q '[:find ?age
;          :in $ ?name
;          :where [?e :user/name ?name]
;                 [?e :user/age ?age]]
;        @conn "alice")

;(data/pull @conn [:user/name :user/age] [:user/id "1"])

;(posh/pull conn [:user/name :user/age] [:user/id "1"])

;(posh/transact! conn [{:user/id "1"
;                       :user/name "hello"
;                       :user/age 27}])