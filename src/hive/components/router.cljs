(ns hive.components.router
  (:require [cljs-react-navigation.base :as base]
            [cljs-react-navigation.reagent :as reagent]
            [hive.rework.core :as work]))

;; This is probably not the cleanest way to do it but it gets the job done :)

;; copied from cljs-react-navigation.re-frame
;; https://github.com/seantempesta/cljs-react-navigation/blob/master/src/cljs_react_navigation/re_frame.cljs
(def ref-getStateForAction (atom nil)) ;; HACK

;; TODO: is this even needed ?
;(reg-event-db
;  ::dispatch
;  [trim-v]
;  (fn [app-db [dispatch-args]]
;    (let [routing-state (get app-db :routing)
;          type (aget dispatch-args "type")
;          action-fn (get reagent/NavigationActionsMap type)
;          action (action-fn dispatch-args)
;          new-state (@ref-getStateForAction action routing-state)]
;      (assoc app-db :routing new-state))))

(defn navigate
  "same as react navigation navigate but returns a transaction to use
  with Datascript"
  [routing-state routeName params]
  (let [action-fn (get reagent/NavigationActionsMap "Navigation/NAVIGATE")
        action (action-fn #js {:routeName routeName :params params})
        new-state (@ref-getStateForAction action routing-state)]
    [{:react.navigation/name ::router
      :react.navigation/state new-state}]))

(defn go-back
  "same as react navigation goBack but returns a transaction to use with
  Datascript"
  [routing-state routeName]
  (let [action-fn (get reagent/NavigationActionsMap "Navigation/BACK")
        action (action-fn #js {:routeName routeName})
        new-state (@ref-getStateForAction action routing-state)]
    [{:react.navigation/name ::router
      :react.navigation/state new-state}]))

(def state-query '[:find ?state .
                   :where [_ :react.navigation/state ?state]])

(defn- set-routing
  "resets the complete navigation state according to new routes"
  [new-routes]
  [{:react.navigation/name ::router
    :react.navigation/state new-routes}])

(defn router [props]
  (let [root-router               (:router/root props)
        getStateForAction         (aget root-router "router" "getStateForAction")
        getActionForPathAndParams (aget root-router "router" "getActionForPathAndParams")
        act                       (getActionForPathAndParams (:router/init props))
        init                      (getStateForAction act)
        routing-sub               (work/q! state-query)]
    (reset! ref-getStateForAction getStateForAction)
    (fn [props]
      (let [routing-state (or @routing-sub init)]
        [:> root-router
         {:navigation
          (base/addNavigationHelpers
            (clj->js {:state routing-state
                      :dispatch (fn [action]
                                  (let [next-state (getStateForAction action routing-state)]
                                    (println action)
                                    (when (some? next-state)
                                      (work/transact! (set-routing next-state)))))}))}]))))
