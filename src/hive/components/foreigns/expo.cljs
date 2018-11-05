(ns hive.components.foreigns.expo
  (:require [expo :as Expo]
            [hive.assets :as fl]))

(def MapView   Expo/MapView)
(def MapMarker (. MapView -Marker))
(def MapPolyline (. MapView -Polyline))

(def VectorIcons (js/require "@expo/vector-icons"))
(def Ionicons (. VectorIcons -Ionicons))
