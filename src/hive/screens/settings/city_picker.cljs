(ns hive.screens.settings.city-picker
  (:require [hive.rework.core :as work]
            [hive.state.queries :as queries]
            [hive.screens.symbols :as symbols]
            [react-native :as React]
            [expo :as Expo]
            [hive.assets :as assets]))

(defn change-city
  "returns a Datascript transaction to change the user city, an effect
  to store the new setting and an effect to go back in the navigation"
  [props city user]
  (let [goBack (:goBack (:navigation props))]
    [{:db/id user
      :user/city [:city/name (:city/name city)]}
     [goBack]]))

(defn city-entry
  [props city user]
  (let [info @(work/pull! [{:user/city [:city/name]}] user)]
    [:> React/TouchableOpacity {:onPress #(work/transact! (change-city props city user))}
     [:> React/View {:style {:height 55 :borderBottomColor "lightgray"
                             :borderBottomWidth 1 :paddingTop 5}}
      [symbols/PointOfInterest
        [:> assets/Ionicons {:name "md-map" :size 26}]
        [:> React/Text ""]
        [:> React/Text (:city/name city)]
        [:> React/Text {:style {:color "gray"}}
                       (str (:city/region city) ", "
                            (:city/country city))]
        (when (= (:city/name city) (:city/name (:user/city info)))
          [:> assets/Ionicons {:name "ios-checkmark" :size 26}])]]]))

(defn Selector
  [props]
  (let [cities  @(work/q! queries/cities)
        user    @(work/q! queries/user-entity)]
    [:> React/View {:style {:flex 1}}
      [:> React/View {:style {:height 60 :alignItems "center" :justifyContent "center"
                              :backgroundColor "blue"}}
        [:> React/Text {:style {:color "white" :fontSize 20}}
          "Select City"]]
      [:> React/View {:style {:flex 9 :backgroundColor "white"}}
        (for [city cities]
          ^{:key (:city/name city)}
          [city-entry props city user])]]))
