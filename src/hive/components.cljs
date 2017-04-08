(ns hive.components
  (:require [hive.foreigns :as fl]
            [reagent.core :as r]))

(def text-input (r/adapt-react-class (.-TextInput fl/ReactNative)))
(def button (r/adapt-react-class (.-Button fl/ReactNative)))
(def text (r/adapt-react-class (.-Text fl/ReactNative)))
(def view (r/adapt-react-class (.-View fl/ReactNative)))
(def scrollview (r/adapt-react-class (.-ScrollView fl/ReactNative)))
;(def image (r/adapt-react-class (.-Image ReactNative)))
;(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def mapview (r/adapt-react-class (.-MapView fl/MapBox)))


(defn targets-list
  [targets]
  [scrollview {:style {:height (* 30 (count targets))}}
   (for [t targets]
     ^{:key (:id t)}
     [view {:style {:flex 1}}
      [text {:style {:flex 3}} (:title t)]
      [text {:style {:flex 1 :color "lightgray"}} (:subtitle t)]])])
