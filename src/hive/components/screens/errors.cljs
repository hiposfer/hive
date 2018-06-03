(ns hive.components.screens.errors
  (:require [hive.components.foreigns.native-base :as base]
            [hive.components.foreigns.react :as react]
            [hive.foreigns :as fl]
            [oops.core :as oops]
            [hive.rework.util :as tool]
            [hive.services.location :as position]
            [hive.services.raw.location :as location]
            [hive.rework.core :as work :refer-macros [go-try <?]]))

(defn launch-location-settings
  "launch the android location settings hoping that the user enables
  the gps"
  [props]
  (if (= "android" (oops/oget fl/ReactNative "Platform.OS"))
    (let [settings (oops/oget fl/Expo "IntentLauncherAndroid.ACTION_LOCATION_SOURCE_SETTINGS")
          goBack   (:goBack (:navigation props) settings)
          p (oops/ocall fl/Expo "IntentLauncherAndroid.startActivityAsync" settings)
          c (tool/channel p)]
      (go-try
        (let [answer (tool/log! (<? c))
              tx     (<? (location/watch! position/defaults))]
          (work/transact! tx))
        (goBack)
        (catch :default error)))))
          ;(fl/toast! (ex-message error)))))))

(defn user-location
  [props]
  (let [dims (tool/keywordize (oops/ocall fl/ReactNative "Dimensions.get" "window"))]
    [:> base/Container {:style {:paddingVertical "20%"}}
     [:> base/Content
      [:> base/Card
       [:> base/CardItem {:cardBody true}
        [:> react/Image {:style  {:width (* (:width dims) 0.8)
                                  :height (* (:height dims) 0.5)
                                  :resizeMode "contain" :flex 1}
                         :source fl/thumb-run}]]
       [:> base/CardItem
        [:> base/Body {:style {:alignItems "center"}}
         [:> base/Text {:style {:flexWrap "wrap"}}
                       "We couldn't find your current location"]
         [:> base/Text]
         [:> base/Text "Please enable your GPS to continue"]
         [:> react/View {:style {:flexDirection "row" :alignItems "flex-start"
                                 :flex 1}}
          [:> base/Button {:danger true :bordered false
                           :on-press #((:goBack (:navigation props)))}
            [:> base/Icon {:name "ios-close-circle"}]]
          [:> base/Button {:success true :iconRight true :bordered false
                           :on-press #(launch-location-settings props)}
           [:> base/Text "OK"] [:> base/Icon {:name "ios-arrow-forward"}]]]]]]]]))

(defn no-internet
  "display a nice little monster asking for internet connection"
  []
  (let [dims (tool/keywordize (oops/ocall fl/ReactNative "Dimensions.get" "window"))]
    [:> base/Container
     [:> base/Content {:style {:padding 10}}
      [:> base/Card {:style {:width (* (:width dims) 0.95)}}
       [:> base/CardItem {:cardBody true}
        [:> react/Image {:style  {:width (* (:width dims) 0.9)
                                  :height (* (:height dims) 0.8)
                                  :resizeMode "contain" :flex 1}
                         :source fl/thumb-sign}]]]]]))


;hive.rework.state/conn
