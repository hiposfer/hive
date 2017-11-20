(ns hive.components.screens
  (:require [hive.components.core :refer [Drawer Container Header Item View Image
                                          Text Icon Input MapView ListItem Body
                                          Left Content Button Title TouchableHighlight]]
            [hive.foreigns :as fl]
            [hive.components.elements :refer [drawer-menu]]
            [reagent.core :as r]))

"Each Screen will receive two props:
 - screenProps - Extra props passed down from the router (rarely used)
 - navigation  - The main navigation functions in a map as follows:
   {:state     - routing state for this screen
    :dispatch  - generic dispatch fn
    :goBack    - pop's the current screen off the stack
    :navigate  - most common way to navigate to the next screen
    :setParams - used to change the params for the current screen}"

(defn search-bar
  [drawer]
  [:> Header {:searchBar true :rounded true}
   [:> Item {}
    [:> Button {:transparent true :full true
                :on-press #(if (:open? @drawer) (.close (.-_root (:ref @drawer)))
                                               (.open (.-_root (:ref @drawer))))}
     [:> Icon {:name "ios-menu" :transparent true}]]
    [:> Input {:placeholder "Where would you like to go?"}]
    [:> Icon {:name "ios-search"}]]])

(defn home
  [props]
  (let [{:keys [navigate]} (:navigation props)
        dopts  (volatile! {:ref nil :open? false})]
    [:> Drawer {:content (r/as-element (drawer-menu props))
                :type "displace" :tweenDuration 100
                :on-close #(vswap! dopts assoc :open? false)
                :ref #(vswap! dopts assoc :ref %)}
     [:> Container {}
      [search-bar dopts]
      [:> MapView {:initialRegion {:latitude 50.11361
                                   :longitude 8.67972,
                                   :latitudeDelta 0.02,
                                   :longitudeDelta 0.02}
                   :showsUserLocation true
                   :style {:flex 1}}]]]))

;(defn home
;  [{:keys [screenProps navigation] :as props}]
;  (let [;city      (:user/city (om/props this))
;        bbox      [1 2 3 4];(:bbox city); [lon lat lon lat]
;        dlat      (/ (- (nth bbox 2) (nth bbox 0))
;                     6)
;        dlon      (/ (- (nth bbox 3) (nth bbox 1))
;                     6)
;        [lon lat] [1 2]];(:coordinates (:geometry city))]
;    [:> Drawer {:content (r/as-component (drawer-menu props))
;                :open true ;@menu-open?
;                :type    "displace" :tweenDuration 100}
;               ;:onClose (fn [_] (router/dispatch [:view/side-menu false]))}
;      [:> Container {:paddingTop fl/Expo.Constants.statusBarHeight}
;        [:> Header {:searchBar true :rounded true}
;          [:> Item {}
;           [:> Icon {:name "ios-menu" :transparent true}]
;           [:> Input {:placeholder "Where would you like to go?"}]
;           [:> Icon {:name "ios-search"}]]]
;        [:> MapView {:initialRegion {:latitude lat
;                                     :longitude lon,
;                                     :latitudeDelta dlat,
;                                     :longitudeDelta dlon}
;                     :showsUserLocation true
;                     :style {:flex 1}}]]]))

(defn settings
  [{:keys [screenProps navigation] :as props}]
  (let [{:keys [navigate goBack]} navigation]
    [:> TouchableHighlight {:style {:background-color "#999" :padding 10
                                    :border-radius 5}
                            :on-press #(goBack)}
     [:> Text {:style {:color "white" :text-align "center"
                       :font-weight "bold"}}
      "Go Home"]]))


;(defn settings
;    []
;    ;(let [data (:app/settings (om/props this))]
;    [Container {}
;     [Header {}
;      [Left {} [Button {} [Icon {:name "menu"}]]]
;      [Body {} [Title {} (:title data)]]]
;     [Content]])
;       ;[city-selector (:app/cities (om/props this))]]]))

;(defn home
;    "start screen. A simple screen with a map and an input text to search for a place"
;    []
;    [drawer {:content menu :open false;@menu-open?
;             :type "displace" :tweenDuration 100}
;             ;:onClose (fn [_] (router/dispatch [:view/side-menu false]))}
;     [container
;      [header {:searchBar true :rounded true}
;       [item
;        [button {:transparent true :full true}
;                 ;:on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
;         [icon {:name "menu" :transparent true}]]
;        [input {:placeholder  "where would you like to go?"}]
;               ;:ref #(router/dispatch [:user.input/ref %])
;               ;:onChangeText #(router/dispatch [:user.input/place %])}]
;        (if true;(empty? @search-text)
;          [icon {:name "ios-search"}]
;          [button {:transparent true :full true}
;                  ;:on-press #(router/dispatch [:user.input/place ""])}
;           [icon {:name "close"}]])]]
;      #_(when @view-targets?
;          [targets-list @map-markers])
;      #_[mapview {:style                   {:flex 1}
;                  :initialZoomLevel        hive.core/default-zoom
;                  :annotationsAreImmutable true
;                  :initialCenterCoordinate (util/feature->verbose @current-city)
;                  :annotations             (clj->js (map util/feature->annotation @map-markers))
;                  :showsUserLocation       true ;:ref (fn [this] (println "this: " this)) ;(when this (.keys this))))
;                  :onUpdateUserLocation    #(router/dispatch [:user/location (util/verbose->feature (js->clj % :keywordize-keys true))])
;                  :onTap                   #(router/dispatch [:view.home/targets false])
;                  :ref                     #(router/dispatch [:map/ref %])}]
;      #_(when (and @directions (seq @search-text))
;          [button {:full true
;                     :on-press #(router/dispatch [:view/screen :view.screen/directions])}
;           [icon {:name "information-circle" :transparent true}]
;           [text "See trip details"]])
;      [fab {:style {:backgroundColor "red"}}; fab-color}
;           ;:on-press #(router/dispatch [:view.user/location])}
;       [icon {:name "md-locate"}]]]])

;;(defn blockade
;;  "our current splash screen"
;;  []
;;  [container
;;    [content
;;      [spinner {:color "blue"}]
;;      [text "Fetching app information ... please wait"]]])
;
;(defn directions
;    "basic navigation directions"
;    []
;    #_(let [route        (subs/subscribe [:user.goal/route])
;            instructions (sequence (comp (mapcat :steps)
;                                       (map :maneuver)
;                                       (map :instruction)
;                                       (map-indexed vector))
;                                 (:legs @route))])
;    [container
;     [content
;      [card
;       [card-item [icon {:name "flag"}]
;        [text "distance: " 5 " meters"]] ;(:distance @route)
;       [card-item [icon {:name "information-circle"}]
;        [text "duration: " (Math/round (/ 10 60)) " minutes"]] ;(:duration @route)
;       [card-item [icon {:name "time"}]
;        [text "time of arrival: " (str (js/Date. (+ (js/Date.now
;                                                      (* 1000 20))))) ;(:duration @route))))))
;         " minutes"]]]
;      [card
;       [card-item [icon {:name "map"}]
;        [text "Instructions: "]]
;       #_(for [[id text] instructions]
;           (if (= id (first (last instructions)))
;             ^{:key id} [card-item [icon {:name "flag"}]
;                         [text text]]
;             ^{:key id} [card-item [icon {:name "ios-navigate-outline"}]
;                         [text text]]))]]])
;
;(defn missing-internet
;    "display a nice little monster asking for internet connection"
;    []
;    (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
;      [container
;       [content {:style {:padding 10}}
;        [card ;{:style {:width (* (:width dims) 0.95)}}
;         [card-item {:cardBody true}
;          [image {:style {:width (* (:width dims) 0.9)
;                            :height (* (:height dims) 0.8)
;                            :resizeMode "contain" :flex 1}
;                    :source fl/thumb-sign}]]]]]))
;
;(defn user-location-error
;    []
;    (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
;      [container
;       [content {:style {:padding 10}}
;        [card
;         [card-item {:cardBody true}
;          [image {:style {:width (* (:width dims) 0.9)
;                            :height (* (:height dims) 0.7)
;                            :resizeMode "contain" :flex 1}
;                    :source fl/thumb-run}]]
;         [card-item
;          [body
;           [text "ERROR: we couldn't find your current position. This might be due to:"]
;           [text {:style {:textAlign "left"}} "\u2022 no gps connection enabled"]
;           [text "\u2022 bad signal reception"]]]]]]))