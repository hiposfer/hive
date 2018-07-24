(ns hive.components.screens.settings.city-picker
  (:require [hive.rework.core :as work]
            [hive.services.store :as store]
            [hive.queries :as queries]
            [hive.components.symbols :as symbols]
            [hive.components.foreigns.react :as react]
            [hive.components.foreigns.expo :as expo]))

(defn change-city
  "returns a Datascript transaction to change the user city, an effect
  to store the new setting and an effect to go back in the navigation"
  [city props]
  (let [user   (:user/id props)
        goBack (:goBack (:navigation props))
        tx     {:user/id user
                :user/city [:city/name (:city/name city)]}]
    [tx
     [store/save! (select-keys tx [:user/city])]
     [goBack]]))

(defn city-entry
  [props]
  (let [params   (:params (:state (:navigation props)))
        city     (:city props)]
    [:> react/TouchableOpacity {:onPress #(work/transact! (change-city city props))}
     [:> react/View {:style {:height 55 :borderBottomColor "lightgray"
                             :borderBottomWidth 1 :paddingTop 5}}
      [symbols/PointOfInterest
        [:> expo/Ionicons {:name "md-map" :size 26}]
        [:> react/Text ""]
        [:> react/Text (:city/name city)]
        [:> react/Text {:style {:color "gray"}}
          (str (:city/region city) ", " (:city/country city))]
        (when (= (:city/name params) (:city/name city))
          [:> expo/Ionicons {:name "ios-checkmark" :size 26}])]]]))

(defn Selector
  [props]
  (let [params   (:params (:state (:navigation props)))
        cities  @(work/q! queries/cities)
        id       (:user/id params)]
    [:> react/View {:style {:flex 1}}
      [:> react/View {:style {:height 60 :alignItems "center" :justifyContent "center"
                              :backgroundColor "blue"}}
        [:> react/Text {:style {:color "white" :fontSize 20}}
          "Select City"]]
      [:> react/View {:style {:flex 9 :backgroundColor "white"}}
        (for [city cities]
          ^{:key (:city/name city)}
          [city-entry (assoc props :city city :user/id id)])]]))
