(ns hive.components.foreigns.expo
  (:require [hive.foreigns :as fl]))

(def MapView   (.-MapView fl/Expo))
(def MapMarker (.-Marker MapView))
(def MapPolyline (.-Polyline MapView))
;(def Ionicons (.-Ionicons (js/require "@expo/vector-icons")))
