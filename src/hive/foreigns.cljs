(ns hive.foreigns
  "wrapper around javascript native libraries so that requiring them is explicit")

;; NOTE
;; by convention all foreign libraries are defined in MixedCase to show that they
;; dont follow Clojure's dash-naming-convention

;; FIXME
;(set! *warn-on-infer* true)

(defonce ReactNative (js/require "react-native"))
(defonce MapBox      (js/require "react-native-mapbox-gl"))
(defonce NativeBase  (js/require "native-base"))

(defonce app-registry (.-AppRegistry ReactNative))
(defonce async-storage (.-AsyncStorage ReactNative))
(defonce back-handler (.-BackHandler ReactNative))
(defonce toast-android (.-ToastAndroid ReactNative))

(defn on-back-button [f] (.addEventListener back-handler "hardwareBackPress" f))
(defn alert [title] (.alert (.-Alert ReactNative) title))

;; ------ images ------