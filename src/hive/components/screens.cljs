(ns hive.components.screens
  (:require [hive.components.core :refer [Container Header Item Image
                                          Text Icon Input MapView Body
                                          Content Button Title Card
                                          CardItem]]
            [hive.components.elements :as els]
            [hive.queries :as queries]
            [hive.rework.core :as rework]
            [hive.services.geocoding :as geocoding]
            [cljs.core.async :refer-macros [go go-loop]]))

"Each Screen will receive two props:
 - screenProps - Extra props passed down from the router (rarely used)
 - navigation  - The main navigation functions in a map as follows:
   {:state     - routing state for this screen
    :dispatch  - generic dispatch fn
    :goBack    - pop's the current screen off the stack
    :navigate  - most common way to navigate to the next screen
    :setParams - used to change the params for the current screen}"

;; todo: handle autocomplete errors
(defn- search-bar
  [props]
  (let [navigate (:navigate (:navigation props))]
    [:> Header {:searchBar true :rounded true}
     [:> Item {}
      [:> Button {:transparent true :full true
                  :on-press #(navigate "DrawerToggle")}
       [:> Icon {:name "ios-menu" :transparent true}]]
      [:> Input {:placeholder "Where would you like to go?"
                 :onChangeText #(els/autocomplete! {::geocoding/query %
                                                    ::geocoding/mode  "mapbox.places"})}]
      [:> Icon {:name "ios-search"}]]]))

(defn- home-content
  []
  (let [city      @(rework/q! queries/user-city)
        features  @(rework/q! queries/user-places)
        [lon lat]  (:coordinates (:city/geometry city))]
    (if (empty? features)
      [:> MapView {:initialRegion {:latitude lat
                                   :longitude lon
                                   :latitudeDelta 0.02,
                                   :longitudeDelta 0.02}
                   :showsUserLocation true
                   :style {:flex 1}}]
      [els/places features])))

(defn home
  [props]
  [:> Container {}
   [search-bar props]
   [home-content]])

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
      (map els/city-selector @cities (repeat props))]]))

(defn directions
    "basic navigation directions"
    []
    (let [route        (rework/q! queries/route) ;; todo
          instructions (sequence (comp (mapcat :steps)
                                       (map :maneuver)
                                       (map :instruction)
                                       (map-indexed vector))
                                 (:legs @route))]
      [:> Container
       [:> Content
        [:> Card
         [:> CardItem [:> Icon {:name "flag"}]
          [:> Text "distance: " 5 " meters"]] ;(:distance @route)
         [:> CardItem [:> Icon {:name "information-circle"}]
          [:> Text "duration: " (Math/round (/ 10 60)) " minutes"]] ;(:duration @route)
         [:> CardItem [:> Icon {:name "time"}]
          [:> Text "time of arrival: " (str (js/Date. (+ (js/Date.now)
                                                        (* 1000 20)))) ;(:duration @route))))))
           " minutes"]]]
        [:> Card
         [:> CardItem [:> Icon {:name "map"}]
          [:> Text "Instructions: "
           (for [[id text] instructions]
             (if (= id (first (last instructions)))
               ^{:key id} [:> CardItem [:> Icon {:name "flag"}]
                           [text text]]
               ^{:key id} [:> CardItem [:> Icon {:name "ios-navigate-outline"}]
                           [text text]]))]]]]]))
