(ns hive.components.screens.settings.city-picker
  (:require [hive.rework.core :as work]
            [hive.services.store :as store]
            [hive.components.native-base :as base]
            [hive.queries :as queries]
            [clojure.string :as str]
            [hive.components.symbols :as symbols]
            [hive.components.react :as react]))

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
        goBack   (:goBack (:navigation props))
        check    (conj {} (when (= (:city/name params) (:city/name city))
                            [:right-icon "ios-checkmark"]))]
    [:> react/View {:style {:height 55 :borderBottomColor "lightgray"
                            :borderBottomWidth 1 :paddingTop 5}}
     [:> base/Button {:on-press #(move-to! city user goBack)
                      :transparent true}
      [symbols/point-of-interest
       (merge check
              {:left-icon  "md-map"
               :title      (:city/name city)
               :subtitle   (str/join ", " [(:city/region city)
                                           (:city/country city)])})]]]))

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

