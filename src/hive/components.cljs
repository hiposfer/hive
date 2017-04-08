(ns hive.components
  "wrappers around react-native js components"
  (:require [hive.foreigns :as fl]
            [reagent.core :as r]))


(def text-input (r/adapt-react-class (.-TextInput fl/ReactNative)))
(def button (r/adapt-react-class (.-Button fl/ReactNative)))
(def text (r/adapt-react-class (.-Text fl/ReactNative)))
(def view (r/adapt-react-class (.-View fl/ReactNative)))
;(def image (r/adapt-react-class (.-Image ReactNative)))
;(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def mapview (r/adapt-react-class (.-MapView fl/MapBox)))
