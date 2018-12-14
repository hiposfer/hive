(ns hive.screens.settings.city-picker
  (:require [hive.state.queries :as queries]
            [hive.screens.symbols :as symbols]
            [react-native :as React]
            [expo :as Expo]
            [hive.assets :as assets]
            [hive.state.core :as state]
            [datascript.core :as data]))

(defn change-city
  "returns a Datascript transaction to change the user city, an effect
  to store the new setting and an effect to go back in the navigation"
  [props city user]
  (let [goBack (:goBack (:navigation props))]
    [{:db/id     user
      :user/area [:area/name (:area/name city)]}
     [goBack]]))

(defn city-entry
  [props city user]
  (let [info @(state/pull! [{:user/area [:area/name]}] user)]
    [:> React/TouchableOpacity {:onPress #(state/transact! (change-city props city user))}
     [:> React/View {:style {:height 55 :borderBottomColor "lightgray"
                             :borderBottomWidth 1 :paddingTop 5}}
      [symbols/PointOfInterest
        [:> assets/Ionicons {:name "md-map" :size 26}]
        [:> React/Text ""]
        [:> React/Text (:area/name city)]
        [:> React/Text ""]
        (when (= (:area/name city) (:area/name (:user/area info)))
          [:> assets/Ionicons {:name "ios-checkmark" :size 26}])]]]))

(defn Selector
  [props]
  (let [cities  @(state/q! queries/kamal-areas)
        user    @(state/q! queries/user-entity)]
    [:> React/View {:style {:flex 1}}
      [:> React/View {:style {:height 60 :alignItems "center" :justifyContent "center"
                              :backgroundColor "blue"}}
        [:> React/Text {:style {:color "white" :fontSize 20}}
          "Select City"]]
      [:> React/View {:style {:flex 9 :backgroundColor "white"}}
        (for [city (map #(data/entity (state/db) %) cities)]
          ^{:key (:area/id city)}
          [city-entry props city user])]]))
