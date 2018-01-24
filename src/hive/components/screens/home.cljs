(ns hive.components.screens.home
  (:require [hive.components.core :refer [Container Header Text Icon MapView Body
                                          Content Button Title Card MapPolyline
                                          CardItem MapMarker View Image Item Input
                                          ListBase ListItem]]
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
            [hive.foreigns :as fl]
            [datascript.core :as data]))

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
             (catch :default _ (navigate "location-error")))))))

(defn- search-bar
  [props features]
  (let [navigate (:navigate (:navigation props))
        ref      (volatile! nil)]
    [:> Header {:searchBar true :rounded true}
     [:> Item {}
      [:> Button {:transparent true :full true
                  :on-press #(navigate "DrawerToggle")}
       [:> Icon {:name "ios-menu" :transparent true}]]
      [:> Input {:placeholder "Where would you like to go?"
                 :ref #(when % (vreset! ref (.-_root %)))
                 :onChangeText #(autocomplete! navigate {::geocoding/query %})}]

      (if (empty? @features)
        [:> Icon {:name "ios-search"}]
        [:> Button {:transparent true :full true
                    :on-press    #(do (.clear @ref) (clear-places!))}
         [:> Icon {:name "close"}]])]]))

(defn- set-goal
  "set feature as the user goal and removes the :user/places attributes from the app
  state"
  [data]
  [{:user/id (:user/id data)
    :user/goal (dissoc data :user/id)}
   [:db.fn/retractAttribute [:user/id (:user/id data)] :user/places]])

(defn- prepare-path
  [goal]
  (if (nil? (:user/position goal))
    (ex-info "missing user location" goal ::user-position-unknown)
    {::directions/coordinates [(:coordinates (:geometry (:user/position goal)))
                               (:coordinates (:geometry goal))]}))

(defn- set-path
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

(def set-path! (work/pipe (work/inject :user/position queries/user-position)
                          prepare-path
                          directions/request!
                          (work/inject :user/id queries/user-id)
                          set-path))

(defn update-map!
  [target]
  (go-try
    (let [places (set-goal (work/inject target :user/id queries/user-id))
          paths  (set-path! target)]
      (work/transact! (concat (<? paths) places)))
    (catch :default error (tool/log! error))))

(defn places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [features]
  [:> ListBase
   (for [target features]
     ^{:key (:id target)}
     [:> ListItem {:on-press #(update-map! target)}
      [:> Body
       [:> Text (:text target)]
       [:> Text {:note true :style {:color "gray"}}
        (str/join ", " (map :text (:context target)))]]])])

(defn directions
  "basic navigation directions"
  [props]
  (let [dirs        @(work/q! queries/user-directions)
        route        (first (:route/routes dirs))
        instructions (sequence (comp (mapcat :steps)
                                     (map :maneuver)
                                     (map :instruction)
                                     (map-indexed vector))
                               (:legs route))]
    [:> Container
     [:> Content
      [:> Card
       [:> CardItem [:> Icon {:name "flag"}]
        [:> Text (str "distance: " (:distance route) " meters")]]
       [:> CardItem [:> Icon {:name "information-circle"}]
        [:> Text "duration: " (Math/round (/ (:duration route) 60)) " minutes"]]
       [:> CardItem [:> Icon {:name "time"}]
        [:> Text (str "time of arrival: " (js/Date. (+ (js/Date.now)
                                                       (* 1000 (:duration route))))
                      " minutes")]]]
      [:> Card
       [:> CardItem [:> Icon {:name "map"}]]
       [:> Text "Instructions: "]
       (for [[id text] instructions]
         ^{:key id} [:> CardItem
                       (if (= id (first (last instructions)))
                         [:> Icon {:name "flag"}]
                         [:> Icon {:name "ios-navigate-outline"}])
                       [:> Text text]])]]]))

(defn home
  "the main screen of the app. Contains a search bar and a mapview"
  [props]
  (let [city      @(work/q! queries/user-city)
        features  (work/q! queries/user-places)
        goal      @(work/q! queries/user-goal)
        route     @(work/q! queries/user-directions)]
    [:> Container {}
     [search-bar props features]
     (if (empty? @features)
       [:> View {:style {:flex 1}}
        [:> MapView {:initialRegion (merge (latlng (:coordinates (:city/geometry city)))
                                           {:latitudeDelta 0.02,
                                            :longitudeDelta 0.02})
                     :showsUserLocation true :style {:flex 1}
                     :showsMyLocationButton true}
         (when goal
           [:> MapMarker {:title       (:text goal)
                          :coordinate  (latlng (:coordinates (:geometry goal)))
                          :description (str/join ", " (map :text (:context goal)))}])
         (when route
           (let [path (map latlng (:coordinates (:geometry (first (:routes route)))))]
             [:> MapPolyline {:coordinates path
                              :strokeColor "#3bb2d0" ;; light
                              :strokeWidth 4}]))]
        (when goal
          [:> Button {:full true :on-press #((:navigate (:navigation props)) "directions")}
           [:> Icon {:name "information-circle" :transparent true}]
           [:> Text (:text goal)]])]
       [places @features])]))

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
                   :drawerIcon (r/as-element [:> Icon {:name "home"}])}))
