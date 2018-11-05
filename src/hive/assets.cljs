(ns hive.assets)

(def ReactNative ^js/ReactNative (js/require "react-native"))

;(defn alert [title] (.. ReactNative -Alert (alert title))

;; ------ images -----
(def thumb-sign (js/require "./resources/images/tb_sign2.png"))
(def thumb-run  (js/require "./resources/images/tbrun1.png"))

(def VectorIcons (js/require "@expo/vector-icons"))
(def Ionicons (. VectorIcons -Ionicons))
