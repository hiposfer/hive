(ns hive.components
  (:require [hive.foreigns :as fl]
            [reagent.core :as r]
            [re-frame.router :as router]
            [reagent.core :as reagent]))

(def text-input (r/adapt-react-class (.-TextInput fl/ReactNative)))
(def button (r/adapt-react-class (.-Button fl/ReactNative)))
(def text (r/adapt-react-class (.-Text fl/ReactNative)))
(def view (r/adapt-react-class (.-View fl/ReactNative)))
(def scrollview (r/adapt-react-class (.-ScrollView fl/ReactNative)))
(def image (r/adapt-react-class (.-Image fl/ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight fl/ReactNative)))
(def mapview (r/adapt-react-class (.-MapView fl/MapBox)))
(def side-menu (r/adapt-react-class fl/SideMenu))


(defn targets-list
  [targets]
  [view {:style {:height (* 55 (count targets))}}
   (for [t targets]
     ^{:key (:id t)}
     [touchable-highlight {:style    {:flex 1}
                           :on-press #(do (router/dispatch [:map/camera (:coordinates t)])
                                          (router/dispatch [:view.home/targets false]))}
       [view {:style {:flex 1 :borderBottomColor "lightblue" :borderWidth 1}}
         [text {:style {:flex 1}} (:title t)]
         [text {:style {:flex 1 :color "gray"}} (:subtitle t)]]])])

(def home-img (js/require "./images/ic_home.png"))
(def settings-img (js/require "./images/ic_settings.png"))

(def menu
  [scrollview
    [view {:flex-direction "row"} [image {:source home-img}] [text "HOME"]]
    [view {:flex-direction "row"} [image {:source settings-img}] [text "SETTINGS"]]])

