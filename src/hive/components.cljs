(ns hive.components
  (:require [hive.foreigns :as fl]
            [reagent.core :as r]
            [re-frame.router :as router]
            [reagent.core :as reagent]
            [re-frame.subs :as subs]))

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
                           :on-press (fn [] (router/dispatch [:map/directions (:coordinates t)
                                                              #(router/dispatch [:user/goal %])]))}
       [view {:style {:flex 1 :borderBottomColor "lightblue" :borderWidth 1}}
         [text {:style {:flex 1}} (:title t)]
         [text {:style {:flex 1 :color "gray"}} (:subtitle t)]]])])

(defn menu []
  (let [screen (subs/subscribe [:view/screen])
        go-home (fn [] (when-not (= @screen :home)
                         (router/dispatch [:view/screen :home])))
        go-fix  (fn [] (when-not (= @screen :setting)
                         (router/dispatch [:view/screen :setting])
                         (router/dispatch [:view/side-menu false])))]
    [view
      [touchable-highlight {:on-press go-home}
        [view {:flex-direction "row"}
          [image {:source fl/home-img}]
          [text "HOME"]]]
      [touchable-highlight {:on-press go-fix}
        [view {:flex-direction "row"}
          [image {:source fl/settings-img}]
          [text "SETTINGS"]]]]))

(defn city-selector
  [cities]
  (let [current (subs/subscribe [:user/city])]
    [scrollview
      (for [[id city] cities]
        ^{:key id}
        [touchable-highlight {:on-press #(when-not (= (:city @current) city)
                                           (router/dispatch [:user/city city])
                                           (router/dispatch [:view/screen :home]))}
          [view {:style {:flex 1 :borderBottomColor "lightblue" :borderWidth 1}}
            [text (:name city)]
            [text {:style {:color "gray"}} (str (:region city) ", " (:country city))]]])]))