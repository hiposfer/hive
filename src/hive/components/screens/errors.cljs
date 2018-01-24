(ns hive.components.screens.errors
  (:require [hive.components.core :refer [Container Text Body Content Card
                                          CardItem Image Button Icon View]]
            [hive.foreigns :as fl]
            [hive.rework.util :as tool]
            [hive.services.location :as position]
            [hive.services.raw.location :as location]
            [hive.rework.core :as work :refer-macros [go-try <?]]))

(defn launch-location-settings
  "launch the android location settings hoping that the user enables
  the gps"
  [props]
  (if (= "android" (:OS fl/Platform))
    (let [launch         (:startActivityAsync fl/IntentLauncherAndroid)
          navigate-back! (:goBack (:navigation props))
          p (launch (:ACTION_LOCATION_SOURCE_SETTINGS fl/IntentLauncherAndroid))
          c (tool/channel p)]
      (go-try
        (let [answer (tool/log! (<? c))
              tx     (<? (location/watch! position/defaults))]
          (work/transact! tx))
        (navigate-back!)
        (catch :default error
          (fl/toast! (ex-message error)))))))

(defn user-location
  [props]
  (let [dims (js->clj (.get fl/dimensions "window") :keywordize-keys true)]
    [:> Container {:style {:paddingVertical "20%"}}
     [:> Content
      [:> Card
       [:> CardItem {:cardBody true}
        [:> Image {:style  {:width (* (:width dims) 0.8)
                            :height (* (:height dims) 0.5)
                            :resizeMode "contain" :flex 1}
                   :source fl/thumb-run}]]
       [:> CardItem
        [:> Body {:style {:alignItems "center"}}
         [:> Text {:style {:flexWrap "wrap"}}
                  "We couldn't find your current location"]
         [:> Text]
         [:> Text "Please enable your GPS to continue"]
         [:> View {:style {:flexDirection "row" :alignItems "flex-start"
                           :flex 1}}
          [:> Button {:danger true :bordered false
                      :on-press #((:goBack (:navigation props)))}
            [:> Icon {:name "ios-close-circle"}]]
          [:> Button {:success true :iconRight true :bordered false
                      :on-press #(launch-location-settings props)}
           [:> Text "OK"] [:> Icon {:name "ios-arrow-forward"}]]]]]]]]))



(defn no-internet
  "display a nice little monster asking for internet connection"
  []
  (let [dims (js->clj (.get fl/dimensions "window") :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card {:style {:width (* (:width dims) 0.95)}}
       [:> CardItem {:cardBody true}
        [:> Image {:style  {:width (* (:width dims) 0.9)
                            :height (* (:height dims) 0.8)
                            :resizeMode "contain" :flex 1}
                   :source fl/thumb-sign}]]]]]))


;hive.rework.state/conn
