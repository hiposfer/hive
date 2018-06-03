(ns hive.components.screens.settings.city-picker
  (:require [hive.rework.core :as work]
            [hive.services.store :as store]
            [hive.components.foreigns.native-base :as base]
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

(defn city-selector
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
        id       (:user/id params)
        goBack   (:goBack (:navigation props))]
    [:> base/Container
     [:> base/Header
      [:> base/Left
       [:> base/Button {:onPress #(goBack)}
        [:> base/Icon {:name "arrow-back"}]]]
      [:> base/Body
       [:> base/Title "Select City"]]]
     [:> react/View {:style {:flex 1 :backgroundColor "white"}}
      (for [city cities]
        ^{:key (:city/name city)}
        [city-selector (assoc props :city city :user/id id)])]]))

