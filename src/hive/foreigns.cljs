(ns hive.foreigns
  "wrapper around javascript native libraries so that requiring them is explicit")

;; NOTE
;; by convention all foreign libraries are defined in MixedCase to show that they
;; dont follow Clojure's dash-naming-convention

;; FIXME
;(set! *warn-on-infer* true)

(def ReactNative (js/require "react-native"))
(def MapBox (js/require "react-native-mapbox-gl"))
(def FireBase (js/require "firebase"))

(def app-registry (.-AppRegistry ReactNative))
(def back-android (.-BackAndroid ReactNative))
(defn on-back-button [f] (.addEventListener back-android "hardwareBackPress" f))

(defn alert [title] (.alert (.-Alert ReactNative) title))
