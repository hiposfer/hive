(ns hive.foreigns
  (:require [oops.core :as oops]))

(def React       (js/require "react"))
(def Expo        (js/require "expo"))
(def ReactNative (js/require "react-native"))
(def NativeBase  (js/require "native-base"))
(def JwtDecode   (js/require "jwt-decode"))
(def ReactNavigation (js/require "react-navigation"))

(def AuthSession (oops/oget Expo "AuthSession"))

(def Store (js->clj (.-SecureStore Expo)
                    :keywordize-keys true))

(defn alert [title] (.alert (.-Alert ReactNative) title))

;; ------ images -----
(def thumb-sign (js/require "./assets/images/tb_sign2.png"))
(def thumb-run  (js/require "./assets/images/tbrun1.png"))

;; ----- config files ----

(def init-config (js->clj (js/require "./assets/init.json")
                   :keywordize-keys true))
