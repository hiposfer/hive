(ns hive.components.router
  (:require [cljs-react-navigation.base :as base]
            [cljs-react-navigation.reagent :as reagent]
            [hive.rework.core :as work]))

;; copied from cljs-react-navigation.re-frame
;; https://github.com/seantempesta/cljs-react-navigation/blob/master/src/cljs_react_navigation/re_frame.cljs

;; This is probably not the cleanest way to do it but it gets the job done :)

(def state-query '[:find ?state .
                   :where [_ :react.navigation/state ?state]])

(def data-query '[:find [?state ?router]
                  :where [_ :react.navigation/state ?state]
                         [_ :react.navigation/router ?router]])

(defn- set-routing
  "resets the complete navigation state according to new routes"
  [router new-routes]
  [{:react.navigation/name ::router
    :react.navigation/router router
    :react.navigation/state new-routes}])

;(type (get reagent/NavigationActionsMap "Navigation/BACK"))

(defn goBack
  [[state router]]
  (let [back              (get reagent/NavigationActionsMap "Navigation/BACK")
        getStateForAction (aget router "router" "getStateForAction")
        new-state         (getStateForAction (back) state)]
    (set-routing router new-state)))

(defn- dispatch
  [state root-router action]
  (let [getStateForAction (aget root-router "router" "getStateForAction")
        next-state        (getStateForAction action state)]
    ;(println action)
    (when (some? next-state) ;; nil on DrawerClose
      (work/transact! (set-routing root-router next-state)))))

(defn router [props]
  (let [root-router               (:root props)
        getActionForPathAndParams (aget root-router "router" "getActionForPathAndParams")
        getStateForAction         (aget root-router "router" "getStateForAction")
        act                       (getActionForPathAndParams (:init props))
        init                      (getStateForAction act)
        routing-sub               (work/q! state-query)
        routing-state             (or @routing-sub init)
        add-listener              (fn [a] a)] ;; HACK !! I am not sure how to handle this
    [:> root-router
        {:navigation
         (base/addNavigationHelpers
           (clj->js {:state routing-state
                     :addListener add-listener
                     :dispatch #(dispatch routing-state root-router %)}))}]))
