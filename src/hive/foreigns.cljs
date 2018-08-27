(ns hive.foreigns)

(def React       ^js/React (js/require "react"))
(def Expo        ^js/Expo (js/require "expo"))
(def ReactNative ^js/ReactNative (js/require "react-native"))
(def ReactNavigation (js/require "react-navigation"))

(def AuthSession (. Expo -AuthSession))

;(defn alert [title] (.. ReactNative -Alert (alert title))

;; ------ images -----
(def thumb-sign (js/require "./assets/images/tb_sign2.png"))
(def thumb-run  (js/require "./assets/images/tbrun1.png"))
