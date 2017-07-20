(ns hive.foreigns
  "wrapper around javascript native libraries so that requiring them is explicit")

;; NOTE
;; by convention all foreign libraries are defined in MixedCase to show that they
;; dont follow Clojure's dash-naming-convention


(defonce ReactNative (js/require "react-native"))
(defonce MapBox      (js/require "react-native-mapbox-gl"))
(defonce NativeBase  (js/require "native-base"))
(defonce FireBase    (js/require "firebase"))

(defonce app-registry (.-AppRegistry ReactNative))
(defonce async-storage (.-AsyncStorage ReactNative))
(defonce back-handler (.-BackHandler ReactNative))
(defonce toast-android (.-ToastAndroid ReactNative))
(defonce dimensions (.-Dimensions ReactNative))
(defonce net-info    (.-NetInfo ReactNative))

(defn on-back-button [f] (.addEventListener back-handler "hardwareBackPress" f))
(defn alert [title] (.alert (.-Alert ReactNative) title))
(defn on-internet-change
  [f]
  (-> (.-isConnected net-info)
      (.addEventListener "change" f)))

;; ------ images -----
(defonce thumb-sign (js/require "./images/tb_sign2.png"))
(defonce thumb-run  (js/require "./images/tbrun1.png"))

;; ----- config files ----

(defonce init-config (js->clj (js/require "./images/init.json")
                              :keywordize-keys true))