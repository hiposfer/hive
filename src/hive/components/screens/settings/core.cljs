(ns hive.components.screens.settings.core
  (:require [hive.rework.core :as work :refer-macros [go-try <?]]
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
        navigate (:navigate (:navigation props))]
    [:> react/View {:style {:flex 1}}
     [:> react/View {:style {:height 60 :alignItems "center" :justifyContent "center"
                             :backgroundColor "blue"}}
      [:> react/Text {:style {:color "white" :fontSize 20}}
        "Settings"]]
     ;; USER INFO ---------------
     [:> react/View {:style {:height 125 :paddingBottom 20 :paddingTop 20}}
       [:> react/View {:style {:paddingLeft 20 :justifyContent "flex-end"}}
         [:> react/Text {:style {:color "slategray" :fontSize 15}}
                        (str/upper-case "user info")]]
       [:> react/View {:style {:height 50 :flex-direction "row"}}
         [:> react/View {:style {:width 50 :height 50 :margin 5
                                 :alignItems "center" :justifyContent "center"
                                 :backgroundColor "lightgray" :borderRadius 50/2}}
           [:> expo/Ionicons {:name "ios-person-outline" :size 30}]]
         [:> react/View {:style {:flex 0.7 :justifyContent "flex-end"}}
           [:> react/Text "USERNAME"]
           [:> react/Text {:note true :style {:color "gray"}}
                          "email"]]]]
      ;; USER CITY ---------------
     [:> react/View {:style {:height 125}}
      [:> react/Text {:style {:color "slategray" :fontSize 15
                              :paddingLeft 20 :justifyContent "flex-end"}}
                     (str/upper-case "current city")]
      [:> react/TouchableOpacity
       {:onPress #(navigate "select-city"
                            {:user/id (:user/id params)
                             :city/name (:city/name (:user/city city))})
        :style {:height 45}}
       [symbols/point-of-interest
         [:> expo/Ionicons {:name "md-map" :size 26}]
         [:> react/Text ""]
         [:> react/Text (:city/name (:user/city city))]
         [:> react/Text {:style {:color "gray"}}
           (str (:city/region (:user/city city)) ", "
                (:city/country (:user/city city)))]
         [:> expo/Ionicons {:name "ios-checkmark" :size 26}]]]]]))


(def SelectCity (rn-nav/stack-screen cities/selector
                  {:title "Select City"}))

(def Screen (rn-nav/stack-screen settings
              {:title "settings"}))
