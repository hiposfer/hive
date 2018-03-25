(ns hive.components.router
  (:require [cljs-react-navigation.base :as base]
            [cljs-react-navigation.reagent :as reagent]
            [hive.rework.core :as work]))

;; copied from cljs-react-navigation.re-frame
;; https://github.com/seantempesta/cljs-react-navigation/blob/master/src/cljs_react_navigation/re_frame.cljs

;; This is probably not the cleanest way to do it but it gets the job done :)

(def state-query '[:find ?state .
                   :where [_ :react.navigation/state ?state]])

(defn- set-routing
  "resets the complete navigation state according to new routes"
  [new-routes]
  [{:react.navigation/name ::router
    :react.navigation/state new-routes}])

(defn- dispatch
  [state getStateForAction action]
  (let [next-state (getStateForAction action state)]
    (println action)
    (when (some? next-state) ;; nil on DrawerClose
      (work/transact! (set-routing next-state)))))

(defn router [props]
  (let [root-router               (:router/root props)
        getStateForAction         (aget root-router "router" "getStateForAction")
        getActionForPathAndParams (aget root-router "router" "getActionForPathAndParams")
        act                       (getActionForPathAndParams (:router/init props))
        init                      (getStateForAction act)
        routing-sub               (work/q! state-query)
        routing-state (or @routing-sub init)]
    [:> root-router
        {:navigation
         (base/addNavigationHelpers
           (clj->js {:state routing-state
                     :dispatch #(dispatch routing-state getStateForAction %)}))}]))
