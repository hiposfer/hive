(ns hive.components
  (:require [hive.foreigns :as fl]
            [reagent.core :as r]
            [re-frame.router :as router]
            [reagent.core :as reagent]
            [re-frame.subs :as subs]
            [hive.util :as util]))

(def view (r/adapt-react-class (.-View fl/ReactNative)))

(def container (r/adapt-react-class (.-Container fl/NativeBase)))
(def header    (r/adapt-react-class (.-Header fl/NativeBase)))
(def footer    (r/adapt-react-class (.-Footer fl/NativeBase)))
(def left      (r/adapt-react-class (.-Left fl/NativeBase)))
(def right     (r/adapt-react-class (.-Right fl/NativeBase)))
(def card      (r/adapt-react-class (.-Card fl/NativeBase)))
(def card-item (r/adapt-react-class (.-CardItem fl/NativeBase)))
(def list-base (r/adapt-react-class (.-List fl/NativeBase)))
(def list-item (r/adapt-react-class (.-ListItem fl/NativeBase)))
(def button    (r/adapt-react-class (.-Button fl/NativeBase)))
(def icon      (r/adapt-react-class (.-Icon fl/NativeBase)))
(def body      (r/adapt-react-class (.-Body fl/NativeBase)))
(def item      (r/adapt-react-class (.-Item fl/NativeBase)))
(def content   (r/adapt-react-class (.-Content fl/NativeBase)))
(def spinner   (r/adapt-react-class (.-Spinner fl/NativeBase)))
(def input     (r/adapt-react-class (.-Input fl/NativeBase)))
(def drawer    (r/adapt-react-class (.-Drawer fl/NativeBase)))
(def text      (r/adapt-react-class (.-Text fl/NativeBase)))
(def title     (r/adapt-react-class (.-Title fl/NativeBase)))

(def mapview (r/adapt-react-class (.-MapView fl/MapBox)))


(defn targets-list
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [features]
  [list-base
   (for [target features]
     ^{:key (:id target)}
     [list-item {:on-press #(router/dispatch [:map/directions target :user/goal])}
       [body
         [text (:title target)]
         [text {:note true :style {:color "gray"}} (:subtitle target)]]])])

(defn menu
  "side menu for the user to choose where to navigate to in Android"
  []
  (let [screen (subs/subscribe [:view/screen])
        go-home (fn [] (when-not (= @screen :home)
                         (router/dispatch [:view/screen :home])))
        go-fix  (fn [] (when-not (= @screen :setting)
                         (router/dispatch [:view/screen :setting])
                         (router/dispatch [:view/side-menu false])))]
    [view {:activeOpacity 1}
      [button {:full true :on-press go-home}
        [icon {:name "home"}]
        [text "HOME"]]
      [button {:full true :on-press go-fix}
        [icon {:name "settings"}]
        [text "SETTINGS"]]]))

(defn city-selector
  "list of cities currently supported, displayed to the user to manually change his
   region of interest"
  [cities]
  (let [current (subs/subscribe [:user/city])]
    [list-base
      (for [[id city] cities]
        ^{:key id}
        [list-item {:on-press #(when-not (= (:city @current) city
                                            (router/dispatch [:user/city city])
                                            (router/dispatch [:view/screen :home])))}
         [body
           [text (:name city)]
           [text {:note true :style {:color "gray"}}
                 (str (:region city) ", " (:country city))]]])]))
