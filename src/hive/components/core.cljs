(ns hive.components.core
  (:require [hive.foreigns :as fl]))

(def View   (.-View fl/ReactNative))
(def Image  (.-Image fl/ReactNative))
(def Modal  (.-Modal fl/ReactNative))
(def TouchableHighlight (.-TouchableHighlight fl/ReactNative))

(def Container (.-Container fl/NativeBase))
(def Fab       (.-Fab fl/NativeBase))
(def Header    (.-Header fl/NativeBase))
(def Footer    (.-Footer fl/NativeBase))
(def Left      (.-Left fl/NativeBase))
(def Right     (.-Right fl/NativeBase))
(def Card      (.-Card fl/NativeBase))
(def CardItem  (.-CardItem fl/NativeBase))
(def ListBase  (.-List fl/NativeBase))
(def ListItem  (.-ListItem fl/NativeBase))
(def Button    (.-Button fl/NativeBase))
(def Icon      (.-Icon fl/NativeBase))
(def Body      (.-Body fl/NativeBase))
(def Item      (.-Item fl/NativeBase))
(def Content   (.-Content fl/NativeBase))
(def Spinner   (.-Spinner fl/NativeBase))
(def Input     (.-Input fl/NativeBase))
(def Drawer    (.-Drawer fl/NativeBase))
(def Text      (.-Text fl/NativeBase))
(def Title     (.-Title fl/NativeBase))

(def MapView   (.-MapView fl/Expo))
(def MapMarker (.-Marker MapView))
(def MapPolyline (.-Polyline MapView))
