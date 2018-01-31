(ns hive.components.screens.home
  (:require [hive.components.native-base :as base]
            [hive.components.expo :as expo]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.components.navigation :as nav]
            [reagent.core :as r]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [hive.rework.util :as tool]
            [hive.components.screens.errors :as errors]
            [hive.services.directions :as directions]
            [hive.services.geocoding :as geocoding]
            [hive.services.raw.location :as location]
            [hive.services.location :as position]
            [datascript.core :as data]
            [hive.components.react :as react]
            [hive.foreigns :as fl]
            [hive.libs.geometry :as geometry]))

(defn latlng
  [coordinates]
  {:latitude (second coordinates) :longitude (first coordinates)})

(defn- update-places
  "transact the geocoding result under the user id"
  [data]
  [{:user/id (:user/id data)
    :user/places (:features data)}])

(def geocode! (work/pipe (work/inject ::geocoding/proximity queries/user-position)
                         (work/inject ::geocoding/access_token queries/mapbox-token)
                         (work/inject ::geocoding/bbox queries/user-city)
                         #(update % ::geocoding/bbox (fn [c] (:city/bbox c)))
                         geocoding/autocomplete!
                         (work/inject :user/id queries/user-id)
                         update-places))

(defn- clear-places! []
  (work/transact! [{:user/places []
                    :user/id     (work/q queries/user-id)}]))

(defn autocomplete!
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  [navigate query]
  (go-try
    (work/transact! (<? (geocode! query)))
    (catch :default _
      (if (empty? (::geocoding/query query))
        (clear-places!)
        (try (work/transact! (<? (location/watch! position/defaults)))
             (catch :default _
               (.dismiss fl/Keyboard)
               (navigate "location-error")))))))

(defn- search-bar
  [props places]
  (let [navigate (:navigate (:navigation props))
        ref      (volatile! nil)]
    [:> base/Header {:searchBar true :rounded true}
     [:> base/Item {}
      [:> base/Button {:transparent true :full true
                       :on-press #(navigate "DrawerToggle")}
       [:> base/Icon {:name "ios-menu" :transparent true}]]
      [:> base/Input {:placeholder "Where would you like to go?"
                      :ref #(when % (vreset! ref (.-_root %)))
                      :onChangeText #(autocomplete! navigate {::geocoding/query %})}]
      (if (empty? places)
        [:> base/Icon {:name "ios-search"}]
        [:> base/Button {:transparent true :full true
                         :on-press    #(do (.clear @ref) (clear-places!))}
         [:> base/Icon {:name "close"}]])]]))

(defn- set-goal
  "set feature as the user goal and removes the :user/places attributes from the app
  state"
  [data]
  [{:user/id (:user/id data)
    :user/goal (dissoc data :user/id)}
   [:db.fn/retractAttribute [:user/id (:user/id data)] :user/places]])

(defn- prepare-path
  "transform the user/position attribute to the ::directions/coordinates format"
  [goal]
  (if (nil? (:user/position goal))
    (ex-info "missing user location" goal ::user-position-unknown)
    {::directions/coordinates [(:coordinates (:geometry (:user/position goal)))
                               (:coordinates (:geometry goal))]}))

(defn- set-path
  "takes a mapbox directions response and returns a transaction vector
  to use with transact!. Return an exception if no path was found"
  [path]
  (cond
    (not= (:code path) "Ok")
    (ex-info (or (:msg path) "invalid response")
             path ::invalid-response)

    (not (contains? path :uuid))
    (recur (assoc path :uuid (data/squuid)))

    :ok
    [(tool/with-ns :route (dissoc path :user/id))
     {:user/id (:user/id path)
      :user/directions [:route/uuid (:uuid path)]}]))

(def set-path!
  "takes a geocoded feature (target) and queries the path to get there.
  Returns a transaction vector to use with transact!"
  (work/pipe (work/inject :user/position queries/user-position)
             prepare-path
             directions/request!
             (work/inject :user/id queries/user-id)
             set-path))

;(work/q queries/user-directions)

(defn update-map!
  "associates a target and a path to get there with the user"
  [target]
  (go-try
    (let [places (set-goal (work/inject target :user/id queries/user-id))
          paths  (set-path! target)]
      (work/transact! (concat (<? paths) places))
      (.dismiss fl/Keyboard))
    (catch :default error (tool/log! error))))

(defn places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [features]
  (let [position @(work/q! queries/user-position)]
    [:> base/List {:icon true :style {:flex 1}}
     (for [target features
           :let [distance (/ (geometry/haversine position target) 1000)]]
       ^{:key (:id target)}
       [:> base/ListItem {:icon true :on-press #(update-map! target)
                          :style {:height 50 :paddingVertical 30}}
        [:> base/Left
         [:> react/View {:align-items "center"}
          [:> base/Icon {:name "pin"}]
          [:> base/Text {:note true} (str (.toPrecision distance 3) " km")]]]
        [:> base/Body
         [:> base/Text {:numberOfLines 1} (:text target)]
         [:> base/Text {:note true :style {:color "gray"} :numberOfLines 1}
                       (str/join ", " (map :text (:context target)))]]])]))

(defn route-details
  [props id]
  (print "hello")
  (let [route        (first (:route/routes (work/entity [:route/uuid id])))
        instructions (sequence (comp (mapcat :steps)
                                     (map :maneuver)
                                     (map :instruction)
                                     (map-indexed vector))
                               (:legs route))]
    ^{:key id}
    [:> base/Card
     [:> base/CardItem [:> base/Icon {:name "flag"}]
      [:> base/Text (str "distance: " (:distance route) " meters")]]
     [:> base/CardItem [:> base/Icon {:name "information-circle"}]
      [:> base/Text "duration: " (Math/round (/ (:duration route) 60)) " minutes"]]
     [:> base/CardItem [:> base/Icon {:name "time"}]
      [:> base/Text (str "time of arrival: " (js/Date. (+ (js/Date.now)
                                                          (* 1000 (:duration route))))
                         " minutes")]]
     [:> base/CardItem [:> base/Icon {:name "map"}]
      [:> base/Text "Instructions: "]]
     (for [[id text] instructions]
       ^{:key id} [:> base/CardItem
                   (if (= id (first (last instructions)))
                     [:> base/Icon {:name "flag"}]
                     [:> base/Icon {:name "ios-navigate-outline"}])
                   [:> base/Text text]])]))

(defn directions
  "basic navigation directions"
  [props]
  (let [routes        @(work/q! queries/routes-ids)]
    [:> base/Container
     [:> react/View
      [:> base/DeckSwiper {:dataSource routes
                           :renderItem #(r/as-element (route-details props %))}]]]))

(defn home
  "the main screen of the app. Contains a search bar and a mapview"
  [props]
  (let [info      @(work/q! queries/map-info)]
    [:> base/Container
     [search-bar props (:user/places info)]
     (if (not-empty (:user/places info))
       [places (:user/places info)]
       [:> react/View {:style {:flex 1}}
        [:> expo/MapView {:initialRegion (merge (latlng (:coordinates (:city/geometry (:user/city info))))
                                           {:latitudeDelta 0.02,
                                            :longitudeDelta 0.02})
                          :showsUserLocation true :style {:flex 1}
                          :showsMyLocationButton true}
         (when (some? (:user/goal info))
           (let [point (latlng (:coordinates (:geometry (:user/goal info))))
                 text  (str/join ", " (map :text (:context (:user/goal info))))]
             [:> expo/MapMarker {:title       (:text (:user/goal info))
                                 :coordinate  point
                                 :description text}]))
         (when (some? (:user/directions info))
           (let [geo (:geometry (first (:route/routes (:user/directions info))))]
             [:> expo/MapPolyline {:coordinates (map latlng (:coordinates geo))
                                   :strokeColor "#3bb2d0" ;; light
                                   :strokeWidth 4}]))]
        (when (some? (:user/goal info))
          [:> base/Button {:full true :on-press #((:navigate (:navigation props)) "directions")}
           [:> base/Icon {:name "information-circle" :transparent true}]
           [:> base/Text (:text (:user/goal info))]])])]))

(def Directions    (rn-nav/stack-screen directions
                     {:title "directions"}))
(def Map           (rn-nav/stack-screen home
                     {:title "map"}))
(def LocationError (rn-nav/stack-screen errors/user-location
                     {:title "location-error"}))
(def Navigator     (rn-nav/stack-navigator
                      {:map        {:screen Map}
                       :directions {:screen Directions}
                       :location-error {:screen LocationError}}
                      {:headerMode "none"}))

(def Screen     (nav/drawer-screen Navigator
                  {:title      "Home"
                   :drawerIcon (r/as-element [:> base/Icon {:name "home"}])}))
