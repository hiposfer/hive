(ns hive.components.foreigns.react
  (:require [hive.foreigns :as fl]))

(def View   (. fl/ReactNative -View))
(def Image  (. fl/ReactNative -Image))
(def Modal  (. fl/ReactNative -Modal))
(def Text   (. fl/ReactNative -Text))
(def Button (. fl/ReactNative -Button))

(def TouchableOpacity (. fl/ReactNative -TouchableOpacity))
(def TouchableHighlight (. fl/ReactNative -TouchableHighlight))
(def TouchableWithoutFeedback (. fl/ReactNative -TouchableWithoutFeedback))

(def ScrollView (. fl/ReactNative -ScrollView))

(def Input (. fl/ReactNative -TextInput))

(def FlatList (. fl/ReactNative -FlatList))

(def ActivityIndicator (. fl/ReactNative -ActivityIndicator))
