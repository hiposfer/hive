(ns hive.components
  (:require [hive.foreigns :as fl]
            [reagent.core :as r]
            [re-frame.router :as router]
            [re-frame.subs :as subs]))

(defonce view       (r/adapt-react-class (.-View fl/ReactNative)))
(defonce image      (r/adapt-react-class (.-Image fl/ReactNative)))

(defonce container (r/adapt-react-class (.-Container fl/NativeBase)))
(defonce header    (r/adapt-react-class (.-Header fl/NativeBase)))
(defonce footer    (r/adapt-react-class (.-Footer fl/NativeBase)))
(defonce left      (r/adapt-react-class (.-Left fl/NativeBase)))
(defonce right     (r/adapt-react-class (.-Right fl/NativeBase)))
(defonce card      (r/adapt-react-class (.-Card fl/NativeBase)))
(defonce card-item (r/adapt-react-class (.-CardItem fl/NativeBase)))
(defonce list-base (r/adapt-react-class (.-List fl/NativeBase)))
(defonce list-item (r/adapt-react-class (.-ListItem fl/NativeBase)))
(defonce button    (r/adapt-react-class (.-Button fl/NativeBase)))
(defonce icon      (r/adapt-react-class (.-Icon fl/NativeBase)))
(defonce body      (r/adapt-react-class (.-Body fl/NativeBase)))
(defonce item      (r/adapt-react-class (.-Item fl/NativeBase)))
(defonce content   (r/adapt-react-class (.-Content fl/NativeBase)))
(defonce spinner   (r/adapt-react-class (.-Spinner fl/NativeBase)))
(defonce input     (r/adapt-react-class (.-Input fl/NativeBase)))
(defonce drawer    (r/adapt-react-class (.-Drawer fl/NativeBase)))
(defonce text      (r/adapt-react-class (.-Text fl/NativeBase)))
(defonce title     (r/adapt-react-class (.-Title fl/NativeBase)))

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
