(ns hive.components.screens.settings.city-picker
  (:require [hive.rework.core :as work]
            [hive.queries :as queries]
            [hive.components.symbols :as symbols]
            [hive.components.foreigns.react :as react]
            [hive.components.foreigns.expo :as expo]))

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
    [:> react/TouchableOpacity {:onPress #(work/transact! (change-city props city user))}
     [:> react/View {:style {:height 55 :borderBottomColor "lightgray"
                             :borderBottomWidth 1 :paddingTop 5}}
      [symbols/PointOfInterest
        [:> expo/Ionicons {:name "md-map" :size 26}]
        [:> react/Text ""]
        [:> react/Text (:city/name city)]
        [:> react/Text {:style {:color "gray"}}
                       (str (:city/region city) ", "
                            (:city/country city))]
        (when (= (:city/name city) (:city/name (:user/city info)))
          [:> expo/Ionicons {:name "ios-checkmark" :size 26}])]]]))

(defn Selector
  [props]
  (let [cities  @(work/q! queries/cities)
        user    @(work/q! queries/user-entity)]
    [:> react/View {:style {:flex 1}}
      [:> react/View {:style {:height 60 :alignItems "center" :justifyContent "center"
                              :backgroundColor "blue"}}
        [:> react/Text {:style {:color "white" :fontSize 20}}
          "Select City"]]
      [:> react/View {:style {:flex 9 :backgroundColor "white"}}
        (for [city cities]
          ^{:key (:city/name city)}
          [city-entry props city user])]]))
