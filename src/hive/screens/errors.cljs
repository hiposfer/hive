(ns hive.screens.errors
  (:require [react-native :as React]
            [expo :as Expo]
            [hive.assets :as assets]
            [hive.utils.miscelaneous :as misc]
            [hive.state :as state]))

(defn- on-enable-gps-pressed
  "launch the android location settings hoping that the user enables the gps"
  [goBack]
  (if (= "android" React/Platform.OS)
    (let [settings Expo/IntentLauncherAndroid.ACTION_LOCATION_SOURCE_SETTINGS]
      [[Expo/IntentLauncherAndroid.startActivityAsync settings]
       [goBack]])))

(defn LocationUnknown
  [props]
  (let [dims   (misc/keywordize (React/Dimensions.get "window"))
        goBack (:goBack (:navigation props))]
    [:> React/View {:flex 1 :alignItems "center" :justifyContent "center"}
      [:> React/Image {:source assets/thumb-run
                       :style  {:width (* (:width dims) 0.8)
                                :height (* (:height dims) 0.4)
                                :resizeMode "contain"}}]
      [:> React/Text {:style {:width 250 :height 40 :fontSize 16
                              :fontWeight "bold" :textAlign "center"}}
                     "Can you please enable your GPS?"]
      [:> React/View {:paddingTop 25}
        [:> React/TouchableOpacity
          {:on-press #(state/transact! (on-enable-gps-pressed goBack))
           :style {:borderRadius 5 :backgroundColor "#64AE2F"
                   :alignItems "center" :justifyContent "center"
                   :height 40 :width 100}}
         [:> React/Text {:fontSize 18 :style {:color "white"}} "OK"]]]]))

(defn- on-enable-internet-pressed
  "launch the android location settings hoping that the user enables the gps"
  [goBack]
  (if (= "android" React/Platform.OS)
    (let [settings Expo/IntentLauncherAndroid.ACTION_SETTINGS]
      [[Expo/IntentLauncherAndroid.startActivityAsync settings]
       [goBack]])))


(defn InternetMissing
  "display a nice little monster asking for internet connection"
  [props]
  (let [dims   (misc/keywordize (React/Dimensions.get "window"))
        goBack (:goBack (:navigation props))]
    [:> React/View {:flex 1 :alignItems "center" :justifyContent "center"}
       [:> React/Image {:source assets/thumb-sign
                        :style  {:width (* (:width dims) 0.8)
                                 :height (* (:height dims) 0.4)
                                 :resizeMode "contain"}}]
       [:> React/Text {:style {:width 250 :height 40 :fontSize 16 :fontWeight "bold"
                               :textAlign "center"}}
                      "Can you please enable your internet connection?"]
       [:> React/View {:paddingTop 25}
         [:> React/TouchableOpacity
            {:on-press #(state/transact! (on-enable-internet-pressed goBack))
             :style {:borderRadius 5 :backgroundColor "#64AE2F"
                     :alignItems "center" :justifyContent "center"
                     :height 40 :width 100}}
           [:> React/Text {:fontSize 18 :style {:color "white"}} "OK"]]]]))
