(ns hive.components.foreigns.expo
  (:require [hive.foreigns :as fl]))

(def MapView   (-> fl/Expo .-MapView))
(def MapMarker (-> MapView .-Marker))
(def MapPolyline (-> MapView .-Polyline))

(def VectorIcons (js/require "@expo/vector-icons"))
(def Ionicons (-> VectorIcons .-Ionicons))
