(ns hive.components.screens.settings.core
  (:require [hive.rework.core :as work]
            [clojure.string :as str]
            [hive.components.foreigns.react :as react]
            [hive.components.symbols :as symbols]
            [hive.components.foreigns.expo :as expo]
            [hive.queries :as queries]
            [reagent.core :as r]))

(defn SignOrLogIn
  [visible?]
  [:> react/Modal {:animationType "slide"
                   :transparent false
                   :visible visible?}
    [:> react/View
      [:> react/Text "hello world :)"]]])

(defn Settings
  [props]
  (let [id       @(work/q! queries/user-id)
        city     @(work/pull! [{:user/city [:city/name :city/region :city/country]}]
                              [:user/uid id])
        navigate (:navigate (:navigation props))
        visible? (r/atom false)]
    [:> react/View {:flex 1}
    ;; HEADER -----------------
      [:> react/View {:height 60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> react/Text {:style {:color "white" :fontSize 20}}
                       "Settings"]]
    ;; USER INFO ---------------
      [:> react/View {:height 125 :paddingBottom 20 :paddingTop 20}
        [:> react/Text {:style {:color "slategray" :fontSize 15
                                :paddingLeft 20 :justifyContent "flex-end"}}
                       (str/upper-case "user info")]
        [:> react/View {:height 50 :flex-direction "row"}
          [:> react/View {:width 50 :height 50 :margin 5 :alignItems "center"
                          :justifyContent "center" :backgroundColor "lightgray"
                          :borderRadius 50/2}
            [:> expo/Ionicons {:name "ios-person-outline" :size 30}]]
          [:> react/View {:flex 0.7 :justifyContent "flex-end"}
            [:> react/Text "USERNAME"]
            [:> react/Text {:note true :style {:color "gray"}}
                           "email"]]]]
    ;; USER CITY ---------------
      [:> react/View {:height 125}
        [:> react/Text {:style {:color "slategray" :fontSize 15 :paddingLeft 20
                                :justifyContent "flex-end"}}
                       (str/upper-case "current city")]
        [:> react/TouchableOpacity {:onPress #(navigate "select-city"
                                                        {:user/uid  id
                                                         :city/name (:city/name (:user/city city))})
                                    :style {:height 45}}
          [symbols/PointOfInterest
            [:> expo/Ionicons {:name "md-map" :size 26}]
            [:> react/Text ""]
            [:> react/Text (:city/name (:user/city city))]
            [:> react/Text {:style {:color "gray"}}
                           (str (:city/region (:user/city city)) ", "
                                (:city/country (:user/city city)))]
            [:> expo/Ionicons {:name "ios-checkmark" :size 26}]]]]]))

;hive.rework.state/conn
