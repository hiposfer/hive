(ns hive.android.screens
  (:require [re-frame.router :as router]
            [hive.components :as c]
            [reagent.core :as r]
            [re-frame.subs :as subs]
            [hive.foreigns :as fl]
            [hive.util :as util]))

(defn home
  "start screen. A simple screen with a map and an input text to search for a place"
  []
  (let [current-city  (subs/subscribe [:user/city])]
    (fn [];; this is to prevent updating the initial values of the mapview
      (let [map-markers   (subs/subscribe [:map/annotations])
            view-targets? (subs/subscribe [:view.home/targets])
            menu-open?    (subs/subscribe [:view/side-menu])]
        [c/side-menu {:style {:flex 1} :menu (r/as-element (c/menu)) :isOpen @menu-open?
                      :onChange (fn [s] (when-not (= @menu-open? s) (router/dispatch [:view/side-menu s])))}
          [c/view {:style {:height 50} :flexDirection "row" :background-color "teal" :align-items "center"}
            [c/touchable-highlight {:on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
              [c/image {:source fl/menu-img}]]
            ;; TODO: throttle geocoding
            [c/text-input {:style {:flex 9} :placeholderTextColor "white"
                           :placeholder "where would you like to go?"
                           :onChangeText (fn [v] (router/dispatch [:map/geocode v #(router/dispatch [:map/annotations %])]))}]]
          (when @view-targets?
            [c/targets-list @map-markers])
          [c/mapview {:style                   {:flex 3} :initialZoomLevel hive.core/default-zoom :annotationsAreImmutable true
                      :initialCenterCoordinate (util/feature->verbose @current-city) :annotations (clj->js (map util/feature->annotation @map-markers))
                      :showsUserLocation       true ;:ref (fn [this] (println "this: " this)) ;(when this (.keys this))))
                      :onUpdateUserLocation    #(when % (router/dispatch [:user/location (util/verbose->feature (js->clj % :keywordize-keys true))]))
                      :onTap                   #(router/dispatch [:view.home/targets false])
                      :ref                     (fn [mv] (router/dispatch [:map/ref mv]))}]]))))

(defn settings
  "currently only allows the user to change his current city"
  []
  (let [menu-open? (subs/subscribe [:view/side-menu])]
    [c/side-menu {:style {:flex 1} :menu (r/as-element (c/menu)) :isOpen @menu-open?
                  :onChange (fn [s] (when-not (= @menu-open? s) (router/dispatch [:view/side-menu s])))}
      [c/view {:style {:height 50} :flexDirection "row" :background-color "teal" :align-items "center"}
        [c/touchable-highlight {:on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
          [c/view {:flex-direction "row"}
            [c/image {:source fl/menu-img}]
            [c/text {:align-items "center"} "Settings"]]]]
      [c/city-selector hive.core/cities]]))