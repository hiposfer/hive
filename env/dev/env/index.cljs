(ns env.index
  (:require [env.dev :as dev]))

;; undo main.js goog preamble hack
(set! js/window.goog js/undefined)

(-> (js/require "figwheel-bridge")
    (.withModules #js {"jwt-decode" (js/require "jwt-decode"), "./assets/init.json" (js/require "../../../assets/init.json"), "./assets/icons/loading.png" (js/require "../../../assets/icons/loading.png"), "./assets/images/tbrun1.png" (js/require "../../../assets/images/tbrun1.png"), "expo" (js/require "expo"), "./assets/images/cljs.png" (js/require "../../../assets/images/cljs.png"), "./assets/icons/app.png" (js/require "../../../assets/icons/app.png"), "react-native" (js/require "react-native"), "react-navigation" (js/require "react-navigation"), "react" (js/require "react"), "./assets/cities.json" (js/require "../../../assets/cities.json"), "./assets/images/tb_sign2.png" (js/require "../../../assets/images/tb_sign2.png"), "create-react-class" (js/require "create-react-class"), "@expo/vector-icons" (js/require "@expo/vector-icons")}
)
    (.start "main" "expo" "192.168.0.45"))
