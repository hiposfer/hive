(ns hive.components.screens.home.core
  (:require [hive.components.native-base :as base]
            [hive.components.expo :as expo]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.components.navigation :as nav]
            [reagent.core :as r]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [hive.rework.util :as tool]
            [hive.components.screens.home.route :as route]
            [hive.components.screens.errors :as errors]
            [hive.services.geocoding :as geocoding]
            [hive.components.react :as react]
            [hive.foreigns :as fl]
            [hive.libs.geometry :as geometry]
            [hive.services.raw.http :as http]
            [cljs.core.async :as async]))

(defn latlng
  [coordinates]
  {:latitude (second coordinates) :longitude (first coordinates)})

(defn- update-places
  "transact the geocoding result under the user id"
  [data]
  [{:user/id (:user/id data)
    :user/places (:features data)}])

(defn- clear-places
  [id]
  [{:user/places []
    :user/id     id}])

(defn autocomplete
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  [text id data token]
  (let [args  {::geocoding/proximity (:user/position data)
               ::geocoding/access_token token
               ::geocoding/bbox (:city/bbox (:user/city data))}
        args (tool/validate args ::request ::invalid-input)]
    (if (tool/error? args) (async/to-chan [args])
      (let [args (geocoding/defaults args)
            url  (geocoding/autocomplete args)
            xform (comp (map tool/keywordize)
                        (map (work/inject :user/id queries/user-id))
                        (map update-places))]
        [[url] xform]))))
    ;(go-try
    ;  (work/transact! (<? (geocode! text)))
    ;  (catch :default _
    ;    (try (work/transact! (<? (location/watch! position/defaults)))
    ;         (catch :default _
    ;           (.dismiss fl/Keyboard)
    ;           (navigate "location-error")))))))

(defn- search-bar
  [props places]
  (let [navigate (:navigate (:navigation props))
        id       (work/q queries/user-id)
        data     @(work/pull! [:user/position :user/city] [:user/id id])
        token    (work/q queries/mapbox-token)
        ref      (volatile! nil)]
    [:> base/Header {:searchBar true :rounded true}
     [:> base/Item {}
      [:> base/Button {:transparent true :full true
                       :on-press #(navigate "DrawerToggle")}
       [:> base/Icon {:name "ios-menu" :transparent true}]]
      [:> base/Input {:placeholder "Where would you like to go?"
                      :ref #(when % (vreset! ref (.-_root %)))
                      :onChangeText #(if (empty? %) (work/transact! (clear-places id))
                                       (work/transact-chan
                                         (apply http/json! (autocomplete % id data token))))}]
      (if (empty? places)
        [:> base/Icon {:name "ios-search"}]
        [:> base/Button {:transparent true :full true
                         :on-press    #(do (.clear @ref)
                                           (work/transact! (clear-places id)))}
         [:> base/Icon {:name "close"}]])]]))

(defn- set-goal
  "set feature as the user goal and removes the :user/places attributes from the app
  state"
  [data]
  [{:user/id (:user/id data)
    :user/goal (dissoc data :user/id)}
   [:db.fn/retractAttribute [:user/id (:user/id data)] :user/places]])

(defn choose-route!
  "associates a target and a path to get there with the user"
  [props target]
  (go-try
    (let [places (set-goal (work/inject target :user/id queries/user-id))
          path   (route/get-path! target)
          garbage (map #(vector :db.fn/retractEntity [:route/uuid %])
                        (work/q queries/routes-ids))]
      (work/transact! (concat (<? path) places garbage))
      (.dismiss fl/Keyboard)
      ((:navigate (:navigation props)) "directions"))
    (catch :default error (tool/log! error))))

(defn places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props features]
  (let [position @(work/q! queries/user-position)]
    [:> base/List {:icon true :style {:flex 1}}
     (for [target features
           :let [distance (/ (geometry/haversine position target) 1000)]]
       ^{:key (:id target)}
       [:> base/ListItem {:icon true :on-press #(choose-route! props target)
                          :style {:height 50 :paddingVertical 30}}
        [:> base/Left
         [:> react/View {:align-items "center"}
          [:> base/Icon {:name "pin"}]
          [:> base/Text {:note true} (str (.toPrecision distance 3) " km")]]]
        [:> base/Body
         [:> base/Text {:numberOfLines 1} (:text target)]
         [:> base/Text {:note true :style {:color "gray"} :numberOfLines 1}
                       (str/join ", " (map :text (:context target)))]]])]))

(defn home
  "the main screen of the app. Contains a search bar and a mapview"
  [props]
  (let [info      @(work/q! queries/map-info)]
    [:> base/Container
     [search-bar props (:user/places info)]
     (if (not-empty (:user/places info))
       [places props (:user/places info)]
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
          [:> base/Button {:full true
                           :on-press #((:navigate (:navigation props)) "directions")}
           [:> base/Icon {:name "information-circle" :transparent true}]
           [:> base/Text {:numberOfLines 1} (:text (:user/goal info))]])])]))

(def Directions    (rn-nav/stack-screen route/instructions
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

;(work/q queries/routes-ids)
;(work/transact! [[:db.fn/retractEntity [:route/uuid "cjd5qccf5007147p6t4mneh5r"]]])

;(work/pull '[*] [:route/uuid "cjd5rx3pn00qj47p6lc1z7n1v"])
