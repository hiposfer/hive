(ns hive.foreigns)

(def React       (js/require "react"))
(def Expo        (js/require "expo"))
(def ReactNative (js/require "react-native"))
(def NativeBase  (js/require "native-base"))
(def ReactNavigation (js/require "react-navigation"))

;(def VectorIcons (js/require "@expo/vector-icons"));

(def app-registry  (.-AppRegistry ReactNative))
;(def async-storage (.-AsyncStorage ReactNative))
;(def back-handler (.-BackHandler ReactNative))
;(def toast-android (.-ToastAndroid ReactNative))
(def dimensions    (.-Dimensions ReactNative))
;(def net-info      (.-NetInfo ReactNative))

;(defn on-back-button [f] (.addEventListener back-handler "hardwareBackPress" f))
(defn alert [title] (.alert (.-Alert ReactNative) title))
;(defn on-internet-change
;  [f]
;  (-> (.-isConnected net-info)
;      (.addEventListener "change" f)))

;; ------ images -----
(def thumb-sign (js/require "./assets/images/tb_sign2.png"))
(def thumb-run  (js/require "./assets/images/tbrun1.png"))

;; ----- config files ----

(def init-config (js->clj (js/require "./assets/init.json")
                   :keywordize-keys true))