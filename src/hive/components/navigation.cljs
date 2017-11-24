(ns hive.components.navigation
  (:require [cljs.spec.alpha :as s] ;; cljs react navigation imports in that order. Side effects !!
            [cljs-react-navigation.base :as nav-base]
            [cljs-react-navigation.reagent :as nav]))

;; Screen Navigation Options
(s/def :react-navigation.drawerNavigationOptions/title string?)

(s/def :react-navigation.drawerNavigationOptions/drawerLabel
       nav/string-or-react-element?)

(s/def :react-navigation.drawerNavigationOptions/drawerIcon
      nav/fn-or-react-element?)

(s/def :react-navigation.drawerNavigationOptions/drawerLockMode
        #{"unlocked" "locked-closed" "locked-open"})

(s/def :react-navigation.drawerNavigationOptions/all
  (s/keys :opt-un [:react-navigation.drawerNavigationOptions/title
                   :react-navigation.drawerNavigationOptions/drawerLabel
                   :react-navigation.drawerNavigationOptions/drawerIcon
                   :react-navigation.drawerNavigationOptions/drawerLockMode]))

(defn- navigation-options? [map-or-fn]
  (cond
    (map? map-or-fn) (s/conform :react-navigation.drawerNavigationOptions/all map-or-fn)
    (fn? map-or-fn) (fn [props]
                      (clj->js
                        (s/conform :react-navigation.drawerNavigationOptions/all
                          (map-or-fn (js->clj props :keywordize-keys true)))))
    :else :cljs.spec.alpha/invalid))

(s/def :react-navigation/drawerNavigationOptions (s/nilable (s/conformer navigation-options?)))

(defn drawer-screen [react-component navigationOptions]
  (let [react-component-conformed (s/conform :react/component react-component)
        navigationOptions-conformed (s/conform :react-navigation/drawerNavigationOptions navigationOptions)]
    (assert (not= react-component-conformed :cljs.spec.alpha/invalid)
            (s/explain-str :react/component react-component))
    (assert (not= navigationOptions-conformed :cljs.spec.alpha/invalid)
            (s/explain-str :react-navigation/drawerNavigationOptions navigationOptions))
    (nav-base/screen react-component-conformed navigationOptions-conformed)))


;; -------------------------------------
;; DrawerNavigator DrawerNavigatorConfig
(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/drawerWith
        #(or (number? %) (fn? %)))

(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/drawerPosition
        #{"left" "right"})

(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/contentComponent
        (s/conformer nav/fn-or-react-component?))

;; TODO :react-navigation.DrawerNavigator.DrawerNavigatorConfig/contentOptions)
(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/useNativeAnimations
        boolean?)

(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/drawerBackgroundColor
        string?)

(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/initialRouteName
        string?)

(s/def :react-navigation.DrawerNavigator.DrawerNavigatorConfig/order
        (s/coll-of string?))

(s/def :react-navigation.DrawerNavigator/DrawerNavigatorConfig
  (s/nilable
    (s/keys :opt-un [:react-navigation.DrawerNavigator.DrawerNavigatorConfig/drawerWith
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/drawerPosition
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/contentComponent
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/useNativeAnimations
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/drawerBackgroundColor
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/initialRouteName
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/order
                     :react-navigation.DrawerNavigator.DrawerNavigatorConfig/paths])))

(defn drawer-navigator [routeConfigs drawerNavigatorConfig]
  (let [routeConfigs-conformed (s/conform :react-navigation/RouteConfigs routeConfigs)
        DrawerNavigatorConfig-conformed (s/conform :react-navigation.DrawerNavigator/DrawerNavigatorConfig
                                                   drawerNavigatorConfig)]
    (assert (not= routeConfigs-conformed :cljs.spec.alpha/invalid)
            (s/explain-str :react-navigation/RouteConfigs routeConfigs))
    (assert (not= DrawerNavigatorConfig-conformed :cljs.spec.alpha/invalid)
            (s/explain-str :react-navigation.DrawerNavigator/DrawerNavigatorConfig drawerNavigatorConfig))
    (if DrawerNavigatorConfig-conformed
      (nav-base/DrawerNavigator (clj->js routeConfigs-conformed) (clj->js DrawerNavigatorConfig-conformed))
      (nav-base/DrawerNavigator (clj->js routeConfigs-conformed)))))
