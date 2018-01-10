(ns hive.foreigns)

(def React       (js/require "react"))
(def Expo        (js/require "expo"))
(def ReactNative (js/require "react-native"))
(def NativeBase  (js/require "native-base"))
(def ReactNavigation (js/require "react-navigation"))

(def Location (js->clj (.-Location Expo) :keywordize-keys true))
(def Constants (js->clj (.-Constants Expo) :keywordize-keys true))
(def Permissions (js->clj (.-Permissions Expo) :keywordize-keys true))
(def Platform (js->clj (.-Platform ReactNative) :keywordize-keys true))

(def app-registry  (.-AppRegistry ReactNative))
;(def async-storage (.-AsyncStorage ReactNative))
;(def toast-android (.-ToastAndroid ReactNative))
(def dimensions    (.-Dimensions ReactNative))

(defn alert [title] (.alert (.-Alert ReactNative) title))

;; ------ images -----
(def thumb-sign (js/require "./assets/images/tb_sign2.png"))
(def thumb-run  (js/require "./assets/images/tbrun1.png"))

;; ----- config files ----

(def init-config (js->clj (js/require "./assets/init.json")
                   :keywordize-keys true))
