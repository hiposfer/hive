(ns hive.components.screens.settings.city-picker
  (:require [hive.rework.core :as work]
            [hive.services.store :as store]
            [hive.queries :as queries]
            [hive.components.symbols :as symbols]
            [hive.components.foreigns.react :as react]
            [hive.components.foreigns.expo :as expo]))

(defn move-to!
  [city user goBack]
  (let [tx {:user/id user
            :user/city [:city/name (:city/name city)]}]
    (work/transact! [tx])
    (store/save! (select-keys tx [:user/city]))
    (goBack)))

(defn city-entry
  [props]
  (let [params   (:params (:state (:navigation props)))
        city     (:city props)
        user     (:user/id props)
        goBack   (:goBack (:navigation props))]
    [:> react/TouchableOpacity {:onPress #(move-to! city user goBack)}
     [:> react/View {:style {:height 55 :borderBottomColor "lightgray"
                             :borderBottomWidth 1 :paddingTop 5}}
      [symbols/point-of-interest
        [:> expo/Ionicons {:name "md-map" :size 26}]
        [:> react/Text ""]
        [:> react/Text (:city/name city)]
        [:> react/Text {:style {:color "gray"}}
          (str (:city/region city) ", " (:city/country city))]
        (when (= (:city/name params) (:city/name city))
          [:> expo/Ionicons {:name "ios-checkmark" :size 26}])]]]))

(defn selector
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

