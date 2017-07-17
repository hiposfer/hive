(ns hive.android.screens
  (:require [re-frame.router :as router]
            [hive.components :as c]
            [reagent.core :as r]
            [re-frame.subs :as subs]
            [hive.util :as util]
            [hive.foreigns :as fl]))

(defn home
  "start screen. A simple screen with a map and an input text to search for a place"
  []
  (let [current-city  (subs/subscribe [:user/city])
        search-text   (subs/subscribe [:user.input/place])]
    (fn [];; this is to prevent updating the initial values of the mapview
      (let [map-markers   (subs/subscribe [:map/annotations])
            view-targets? (subs/subscribe [:view.home/targets])
            menu-open?    (subs/subscribe [:view/side-menu])
            directions    (subs/subscribe [:user.goal/route])]
        [c/drawer {:content (r/as-component (c/menu)) :open @menu-open?
                   :type "displace" :tweenDuration 100
                   :onClose (fn [_] (router/dispatch [:view/side-menu false]))}
          [c/container
            [c/header {:searchBar true :rounded true}
              [c/item
                [c/button {:transparent true :full true
                           :on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
                  [c/icon {:name "menu" :transparent true}]]
                [c/input {:placeholder  "where would you like to go?"
                          :ref #(router/dispatch [:user.input/ref %])
                          :onChangeText #(router/dispatch [:user.input/place %])}]
                (if (empty? @search-text)
                  [c/icon {:name "ios-search"}]
                  [c/button {:transparent true :full true
                             :on-press #(router/dispatch [:user.input/place ""])}
                    [c/icon {:name "close"}]])]]
            (when @view-targets?
              [c/targets-list @map-markers])
            [c/mapview {:style                   {:flex 1}
                        :initialZoomLevel        hive.core/default-zoom
                        :annotationsAreImmutable true
                        :initialCenterCoordinate (util/feature->verbose @current-city)
                        :annotations             (clj->js (map util/feature->annotation @map-markers))
                        :showsUserLocation       true ;:ref (fn [this] (println "this: " this)) ;(when this (.keys this))))
                        :onUpdateUserLocation    #(when % (router/dispatch [:user/location (util/verbose->feature (js->clj % :keywordize-keys true))]))
                        :onTap                   #(router/dispatch [:view.home/targets false])
                        :ref                     #(router/dispatch [:map/ref %])}]
            (when (and @directions (seq @search-text))
              ;[c/footer {:transparent true}
               [c/button {:full true
                          :on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
                [c/icon {:name "information-circle" :transparent true}]
                [c/text {:on-press #(router/dispatch [:view/screen :directions])}
                 "See trip details"]])]]))))

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

(defn blockade
  []
  [c/container
    [c/content
      [c/spinner {:color "blue"}]
      [c/text "Fetching app information ... please wait"]]])

(defn directions
  []
  (let [route        (subs/subscribe [:user.goal/route])
        instructions (sequence (comp (mapcat :steps)
                                     (map :maneuver)
                                     (map :instruction)
                                     (map-indexed vector))
                               (:legs @route))]
    [c/container
     [c/content
      [c/card
       [c/card-item [c/icon {:name "flag"}]
                    [c/text "distance: " (:distance @route) " meters"]]
       [c/card-item [c/icon {:name "information-circle"}]
                    [c/text "duration: " (Math/round (/ (:duration @route) 60)) " minutes"]]
       [c/card-item [c/icon {:name "time"}]
                    [c/text "time of arrival: " (str (js/Date. (+ (js/Date.now)
                                                                  (* 1000 (:duration @route)))))
                            " minutes"]]]
      [c/card
       [c/card-item [c/icon {:name "map"}]
                    [c/text "Instructions: "]]
       (for [[id text] instructions]
         (if (= id (first (last instructions)))
           ^{:key id} [c/card-item [c/icon {:name "flag"}]
                                   [c/text text]]
           ^{:key id} [c/card-item [c/icon {:name "ios-navigate-outline"}]
                                   [c/text text]]))]]]))

(defn missing-internet
  []
  (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
    [c/container
     [c/content {:style {:padding 10}}
      [c/card ;{:style {:width (* (:width dims) 0.95)}}
       [c/card-item {:cardBody true}
        [c/image {:style {:width (* (:width dims) 0.9)
                          :height (* (:height dims) 0.8)
                          :resizeMode "contain" :flex 1}
                  :source fl/thumb-sign}]]]]]))
       ;[c/card-item {:footer true}
       ; [c/body
       ;  [c/text {:style {:fontSize 20 :textAlign "center"}} "Error: enable your internet connection to continue"]]]]]]))

;[c/text (with-out-str (cljs.pprint/pprint instructions))]