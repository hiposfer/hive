(ns hive.components.screens.errors
  (:require [hive.components.core :refer [Container Text Body Content Card
                                          CardItem Image]]
            [hive.foreigns :as fl]))

(defn user-location
  []
  (let [dims (js->clj (.get fl/dimensions "window") :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card
       [:> CardItem {:cardBody true}
        [:> Image {:style  {:width (* (:width dims) 0.9)
                            :height (* (:height dims) 0.7)
                            :resizeMode "contain" :flex 1}
                   :source fl/thumb-run}]]
       [:> CardItem
        [:> Body
         [:> Text "ERROR: we couldn't find your current position. This might be due to:"]
         [:> Text {:style {:textAlign "left"}} "\u2022 no gps connection enabled"]
         [:> Text "\u2022 bad signal reception"]]]]]]))

(defn no-internet
  "display a nice little monster asking for internet connection"
  []
  (let [dims (js->clj (.get fl/dimensions "window") :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card {:style {:width (* (:width dims) 0.95)}}
       [:> CardItem {:cardBody true}
        [:> Image {:style  {:width (* (:width dims) 0.9)
                            :height (* (:height dims) 0.8)
                            :resizeMode "contain" :flex 1}
                   :source fl/thumb-sign}]]]]]))

