(ns hive.components.screens.settings.core
  (:require [hive.rework.core :as work]
            [hive.components.foreigns.react :as react]
            [hive.components.symbols :as symbols]
            [hive.components.foreigns.expo :as expo]
            [hive.queries :as queries]
            [reagent.core :as r]
            [hive.foreigns :as fl]))

(defn- UserIcon
  []
  [:> react/View {:width 50 :height 50 :margin 5 :alignItems "center"
                  :justifyContent "center" :backgroundColor "lightgray"
                  :borderRadius 50/2}
    [:> expo/Ionicons {:name "ios-person-outline" :size 30}]])

(defn SignOrLogIn
  [visible?]
  [:> react/Modal {:animationType "slide" :presentationStyle "overFullScreen"
                   :transparent true :visible @visible?
                   :onRequestClose #(reset! visible? false)}
    [:> react/View {:backgroundColor "rgba(0,0,0,0.5)" :flex 1
                    :alignItems "center" :justifyContent "center"}
      [:> react/View {:backgroundColor "white" :height 200 :width 200
                      :alignItems "center" :justifyContent "center"
                      :borderRadius 5}
        [UserIcon]
        [:> react/Input {:placeholder "example@domain.com"
                         :style {:width 150 :height 50}}]
                        ;:onChangeText #(work/transact! (autocomplete % (work/db) props))]
        (when (not= "android" (. fl/ReactNative -Platform.OS))
          [:> react/TouchableOpacity {:style {:borderRadius 5 :height 44 :width 88
                                              :alignItems "center" :justifyContent "center"}
                                      :onPress #(reset! visible? false)}
            [:> expo/Ionicons {:name "ios-close-circle" :size 26}]])]]])

(defn Settings
  [props]
  (let [id   @(work/q! queries/user-id)
        info @(work/pull! [{:user/city [:city/name :city/region :city/country]}
                           :user/displayName :user/isAnonymous :user/email]
                          [:user/uid id])
        navigate (:navigate (:navigation props))
        visible? (r/atom false)]
    [:> react/View {:flex 1}
    ;; HEADER .....................
      [:> react/View {:height 60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> react/Text {:style {:color "white" :fontSize 20}}
                       "Settings"]]
    ;; USER INFO ...................
      [SignOrLogIn visible?]
    ;; USER OVERVIEW ...............
      [:> react/TouchableOpacity {:style {:height 125 :paddingBottom 20 :paddingTop 20}
                                  :onPress #(reset! visible? true)}
        [:> react/Text {:style {:color "slategray" :fontSize 15
                                :paddingLeft 20 :justifyContent "flex-end"}}
                       "USER INFO"]
        [:> react/View {:height 50 :flex-direction "row"}
          [UserIcon]
          [:> react/View {:flex 0.7 :justifyContent "center"}
            [:> react/Text (or (:user/displayName info)
                               (:user/email info)
                               "ANONYMOUS")]
            (when (not (:user/isAnonymous info))
              [:> react/Text {:style {:color "gray"}}
                             "email"])]]]
    ;; USER CITY .................
      [:> react/View {:height 125}
        [:> react/Text {:style {:color "slategray" :fontSize 15 :paddingLeft 20
                                :justifyContent "flex-end"}}
                       "CURRENT CITY"]
        [:> react/TouchableOpacity {:onPress #(navigate "select-city"
                                                        {:user/uid  id
                                                         :city/name (:city/name (:user/city info))})
                                    :style {:height 45}}
          [symbols/PointOfInterest
            [:> expo/Ionicons {:name "md-map" :size 26}]
            [:> react/Text ""]
            [:> react/Text (:city/name (:user/city info))]
            [:> react/Text {:style {:color "gray"}}
                           (str (:city/region (:user/city info)) ", "
                                (:city/country (:user/city info)))]
            [:> expo/Ionicons {:name "ios-checkmark" :size 26}]]]]]))

;hive.rework.state/conn
