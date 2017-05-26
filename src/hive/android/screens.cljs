(ns hive.android.screens
  (:require [re-frame.router :as router]
            [hive.components :as c]
            [reagent.core :as r]
            [re-frame.subs :as subs]
            [hive.foreigns :as fl]
            [hive.util :as util]))

(defn- search-input [v]
  (let [annotate #(router/dispatch [:map/annotations %])]
    (router/dispatch [:map/geocode v annotate])))

(defn home
  "start screen. A simple screen with a map and an input text to search for a place"
  []
  (let [current-city  (subs/subscribe [:user/city])]
    (fn [];; this is to prevent updating the initial values of the mapview
      (let [map-markers   (subs/subscribe [:map/annotations])
            view-targets? (subs/subscribe [:view.home/targets])
            menu-open?    (subs/subscribe [:view/side-menu])]
        [c/drawer {:content (r/as-component (c/menu)) :open @menu-open?
                   :type "displace" :tweenDuration 100
                   :onClose (fn [s] (router/dispatch [:view/side-menu false]))}
          [c/container
            [c/header {:searchBar true :rounded true}
              [c/item
                [c/button {:transparent true :full true
                           :on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
                  [c/icon {:name "menu" :transparent true}]]
                [c/input {:placeholder "where would you like to go?"
                          :onChangeText search-input}]
                [c/icon {:name "ios-search"}]]]
            (when @view-targets?
              [c/targets-list @map-markers])
            [c/mapview {:style                   {:flex 1} :initialZoomLevel hive.core/default-zoom :annotationsAreImmutable true
                        :initialCenterCoordinate (util/feature->verbose @current-city) :annotations (clj->js (map util/feature->annotation @map-markers))
                        :showsUserLocation       true ;:ref (fn [this] (println "this: " this)) ;(when this (.keys this))))
                        :onUpdateUserLocation    #(when % (router/dispatch [:user/location (util/verbose->feature (js->clj % :keywordize-keys true))]))
                        :onTap                   #(router/dispatch [:view.home/targets false])
                        :ref                     (fn [mv] (router/dispatch [:map/ref mv]))}]]]))))

(defn settings
  "currently only allows the user to change his current city"
  []
  (let [menu-open? (subs/subscribe [:view/side-menu])]
    [c/drawer {:content (r/as-component (c/menu)) :open @menu-open?
               :type "displace" :tweenDuration 100
               :onClose (fn [] (router/dispatch [:view/side-menu false]))}
      [c/container
        [c/header
          [c/left
            [c/button {:on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
              [c/icon {:name "menu"}]]]
          [c/body
            [c/title "Settings"]]]
        [c/content
          [c/city-selector hive.core/cities]]]]))