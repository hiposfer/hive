(ns hive.components.screens
  (:require [hive.components.core :refer [Drawer Container Header Item View Image
                                          Text Icon Input MapView ListItem Body
                                          Left Content Button Title TouchableHighlight]]
            [hive.components.elements :refer [city-selector]]
            [hive.queries :as queries]
            [hive.rework.core :as rework]
            [cljs.core.async :refer-macros [go go-loop]]
            [cljs.core.async :as async]
            [hive.services.http :as http]))

"Each Screen will receive two props:
 - screenProps - Extra props passed down from the router (rarely used)
 - navigation  - The main navigation functions in a map as follows:
   {:state     - routing state for this screen
    :dispatch  - generic dispatch fn
    :goBack    - pop's the current screen off the stack
    :navigate  - most common way to navigate to the next screen
    :setParams - used to change the params for the current screen}"

(defn search-bar
  [props]
  (let [navigate (:navigate (:navigation props))]
    [:> Header {:searchBar true :rounded true}
     [:> Item {}
      [:> Button {:transparent true :full true
                  :on-press #(navigate "DrawerToggle")}
       [:> Icon {:name "ios-menu" :transparent true}]]
      [:> Input {:placeholder "Where would you like to go?"}]
      [:> Icon {:name "ios-search"}]]]))

(defn home
  [props]
  (let [[_ _ geometry] @(rework/q! queries/user-city)
        [lon lat]      (:coordinates geometry)]
     [:> Container {}
      [search-bar props]
      [:> MapView {:initialRegion {:latitude lat
                                   :longitude lon
                                   :latitudeDelta 0.02,
                                   :longitudeDelta 0.02}
                   :showsUserLocation true
                   :style {:flex 1}}]]))

(defn settings
  [props]
  (let [cities (rework/q! queries/cities)
        navigate (:navigate (:navigation props))]
    [:> Container
     [:> Header
       [:> Button {:transparent true :full true
                   :on-press #(navigate "DrawerToggle")}
        [:> Icon {:name "menu"}]]
       [:> Body [:> Title "Settings"]]]
     [:> Content
      (map city-selector @cities (repeat props))]]))

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

;(let [success (async/chan)]
;  (go (rework/using ::http/service http/request!
;        {::http/text "https://google.com"
;         ::http/success success})
;    ;(println (js-keys (.then (.text (async/<! success))
;    ;                         #(println %)})))
;    (let [result (async/<! success)]
;      (cljs.pprint/pprint result))))