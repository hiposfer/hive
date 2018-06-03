(ns hive.components.screens.settings.core
  (:require [hive.components.foreigns.native-base :as base]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [cljs-react-navigation.reagent :as rn-nav]
            [clojure.string :as str]
            [hive.components.foreigns.react :as react]
            [hive.components.symbols :as symbols]
            [hive.components.screens.settings.city-picker :as cities]
            [hive.components.foreigns.expo :as expo]))

(defn settings
  [props]
  (let [params   (:params (:state (:navigation props)))
        city     @(work/pull! [{:user/city [:city/name :city/region :city/country]}]
                              [:user/id (:user/id params)])
        ;_ (println city)
        navigate (:navigate (:navigation props))
        goBack   (:goBack (:navigation props))]
    [:> base/Container
     [:> base/Header
      [:> base/Left
       [:> base/Button {:onPress #(goBack)}
        [:> base/Icon {:name "arrow-back"}]]]
      [:> base/Body
       [:> base/Title "Settings"]]]
     ;; USER INFO ---------------
     [:> react/View {:style {:flex 1}}
      [:> react/View {:style {:paddingBottom 20 :paddingTop 20}}
       [:> react/View {:style {:paddingLeft 20 :justifyContent "flex-end"}}
        [:> base/Text {:style {:color "slategray" :fontSize 15}}
                      (str/upper-case "user info")]]
       [:> react/View {:style {:height 50 :flex-direction "row"}}
        [:> react/View {:style {:width 50 :height 50 :margin 5
                                :alignItems "center" :justifyContent "center"
                                :backgroundColor "lightgray" :borderRadius 50/2}}
         [:> base/Icon {:name "ios-person-outline"}]]
        [:> react/View {:style {:flex 0.7 :justifyContent "flex-end"}}
         [:> base/Text "USERNAME"]
         [:> base/Text {:note true :style {:color "gray"}}
                       "email"]]]]
      ;; USER CITY ---------------
      [:> react/View
       [:> react/View {:style {:paddingLeft 20 :justifyContent "flex-end"}}
        [:> base/Text {:style {:color "slategray" :fontSize 15}}
                      (str/upper-case "current city")]]
       [:> react/View {:style {:height 45}}
        [:> base/Button {:onPress #(navigate "select-city"
                                     {:user/id (:user/id params)
                                      :city/name (:city/name (:user/city city))})
                         :transparent true}
         [symbols/point-of-interest
           [:> expo/Ionicons {:name "md-map" :size 26}]
           [:> react/Text ""]
           [:> react/Text (:city/name (:user/city city))]
           [:> react/Text {:style {:color "gray"}}
             (str (:city/region (:user/city city)) ", "
                  (:city/country (:user/city city)))]
           [:> expo/Ionicons {:name "ios-checkmark" :size 26}]]]]]]]))


(def SelectCity (rn-nav/stack-screen cities/selector
                  {:title "Select City"}))

(def Screen (rn-nav/stack-screen settings
              {:title "settings"}))
