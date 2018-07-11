(ns hive.components.screens.errors
  (:require [hive.components.foreigns.react :as react]
            [hive.foreigns :as fl]
            [oops.core :as oops]
            [hive.rework.util :as tool]
            ;[hive.services.location :as location]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [hive.components.foreigns.expo :as expo]))

;; TODO
;(defn- start-location-intent
;  "attempt to start the Android location settings and start watching the position
;  on success"
;  [settings]
;  (tool/async
;    (oops/ocall fl/Expo "IntentLauncherAndroid.startActivityAsync" settings)
;    tool/bypass-error
;    (map (fn [] [location/watch! position/defaults]))))

(defn- launch-location-settings
  "launch the android location settings hoping that the user enables the gps"
  [props]
  (if (= "android" (oops/oget fl/ReactNative "Platform.OS"))
    (let [settings (oops/oget fl/Expo "IntentLauncherAndroid.ACTION_LOCATION_SOURCE_SETTINGS")
          goBack   (:goBack (:navigation props) settings)]
      [;[start-location-intent settings]
       [goBack]])))

(defn UserLocation
  [props]
  (let [dims   (tool/keywordize (oops/ocall fl/ReactNative "Dimensions.get" "window"))
        goBack (:goBack (:navigation props))]
    [:> react/View {:style {:flex 1 :backgroundColor "white" :paddingVertical "20%"
                            :elevation 5 :shadowColor "#000000"
                            :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                            :shadowOpacity 1.0}}
     [:> react/Image {:style  {:width (* (:width dims) 0.8)
                               :height (* (:height dims) 0.5)
                               :resizeMode "contain" :flex 1}
                      :source fl/thumb-run}]
     [:> react/View {:style {:height 200 :alignItems "center"}}
      [:> react/Text {:style {:flexWrap "wrap"}}
        "We couldn't find your current location"]
      [:> react/Text]
      [:> react/Text "Please enable your GPS to continue"]
      [:> react/View {:style {:flexDirection "row" :alignItems "flex-start" :flex 1}}
       [:> react/TouchableOpacity
         {:style {:borderRadius 5 :backgroundColor "red" :height 40 :width 60
                  :justifyContent "center" :alignItems "center"}
          :on-press #(goBack)}
         [:> expo/Ionicons {:name "ios-close-circle" :size 30}]]
       [:> react/TouchableOpacity
         {:on-press #(run! work/transact! (launch-location-settings props))
          :style {:borderRadius 5 :backgroundColor "lawngreen"
                  :height 40 :width 60 :flexDirection "row"
                  :alignItems "center" :justifyContent "space-around"}}
         [:> expo/Ionicons {:name "ios-checkmark-circle" :size 30}]]]]]))

;; TODO: bring this back when needed
;(defn no-internet
;  "display a nice little monster asking for internet connection"
;  []
;  (let [dims (tool/keywordize (oops/ocall fl/ReactNative "Dimensions.get" "window"))]
;    [:> base/Container
;     [:> base/Content {:style {:padding 10}}
;      [:> base/Card {:style {:width (* (:width dims) 0.95)}}
;       [:> base/CardItem {:cardBody true}
;        [:> react/Image {:style  {:width (* (:width dims) 0.9)
;                                  :height (* (:height dims) 0.8)
;                                  :resizeMode "contain" :flex 1}
;                         :source fl/thumb-sign}]]]]]))


;hive.rework.state/conn
