(ns hive.screens.errors
  (:require [react-native :as React]
            [expo :as Expo]
            [hive.assets :as fl]
            [hive.utils.miscelaneous :as tool]
            [hive.rework.core :as work]
            [hive.assets :as assets]))

(defn- launch-location-settings
  "launch the android location settings hoping that the user enables the gps"
  [goBack]
  (if (= "android" React/Platform.OS)
    (let [settings Expo/IntentLauncherAndroid.ACTION_LOCATION_SOURCE_SETTINGS]
      [[Expo/IntentLauncherAndroid.startActivityAsync settings]
       [goBack]])))

(defn UserLocation
  [props]
  (let [dims   (tool/keywordize (React/Dimensions.get "window"))
        goBack (:goBack (:navigation props))]
    [:> React/View {:flex 1 :backgroundColor "white" :paddingVertical "20%"
                    :elevation 5 :shadowColor "#000000"
                    :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                    :shadowOpacity 1.0}
     [:> React/Image {:style  {:width (* (:width dims) 0.8)
                               :height (* (:height dims) 0.5)
                               :resizeMode "contain" :flex 1}
                      :source fl/thumb-run}]
     [:> React/View {:height 200 :alignItems "center"}
      [:> React/Text {:style {:flexWrap "wrap"}}
        "We couldn't find your current location"]
      [:> React/Text]
      [:> React/Text "Please enable your GPS to continue"]
      [:> React/View {:flexDirection "row" :alignItems "flex-start" :flex 1}
       [:> React/TouchableOpacity
         {:style {:borderRadius 5 :backgroundColor "red" :height 40 :width 60
                  :justifyContent "center" :alignItems "center"}
          :on-press #(goBack)}
         [:> assets/Ionicons {:name "ios-close-circle" :size 30}]]
       [:> React/TouchableOpacity
         {:on-press #(work/transact! (launch-location-settings goBack))
          :style {:borderRadius 5 :backgroundColor "lawngreen"
                  :height 40 :width 60 :flexDirection "row"
                  :alignItems "center" :justifyContent "space-around"}}
         [:> assets/Ionicons {:name "ios-checkmark-circle" :size 30}]]]]]))

;; TODO: bring this back when needed
;(defn no-internet
;  "display a nice little monster asking for internet connection"
;  []
;  (let [dims (tool/keywordize (oops/ocall fl/React "Dimensions.get" "window"))]
;    [:> base/Container
;     [:> base/Content {:style {:padding 10}}
;      [:> base/Card {:style {:width (* (:width dims) 0.95)}}
;       [:> base/CardItem {:cardBody true}
;        [:> React/Image {:style  {:width (* (:width dims) 0.9)
;                                  :height (* (:height dims) 0.8)
;                                  :resizeMode "contain" :flex 1}
;                         :source fl/thumb-sign}]]]]]))


;hive.rework.state/conn
