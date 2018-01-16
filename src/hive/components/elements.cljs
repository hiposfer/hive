(ns hive.components.elements
  (:require [hive.components.core :refer [View Button Icon Text ListItem ListBase
                                          Body Container Content Card CardItem Image
                                          Header Item Input]]
            [hive.rework.core :as work]
            [hive.queries :as queries]
            [hive.foreigns :as fl]
            [hive.services.geocoding :as geocoding]
            [clojure.string :as str]
            [hive.rework.util :as tool]
            [cljs.spec.alpha :as s]
            [hive.services.directions :as directions]))

(defn change-city
  [data]
  [{:user/id (:user/id data)
    :user/city [:city/name (:city/name data)]}])

(defn move-to [city] (change-city (work/inject city :user/id queries/user-id)))

(defn city-selector
  [city props]
  ^{:key (:city/name city)}
  [:> ListItem {:on-press #(do (work/transact! (move-to city))
                               ((:navigate (:navigation props)) "Home"))}
     [:> Body {}
       [:> Text (:city/name city)]
       [:> Text {:note true :style {:color "gray"}}
         (str (:city/region city) ", " (:city/country city))]]])

(defn no-internet
  "display a nice little monster asking for internet connection"
  []
  (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card {:style {:width (* (:width dims) 0.95)}}
       [:> CardItem {:cardBody true}
        [:> Image {:style  {:width (* (:width dims) 0.9)
                            :height (* (:height dims) 0.8)
                            :resizeMode "contain" :flex 1}
                   :source fl/thumb-sign}]]]]]))

(defn user-location-error
  []
  (let [dims (js->clj (. fl/dimensions (get "window")) :keywordize-keys true)]
    [:> Container
     [:> Content {:style {:padding 10}}
      [:> Card
       [:> CardItem {:cardBody true}
        [:> Image {:style {:width (* (:width dims) 0.9)
                           :height (* (:height dims) 0.7)
                           :resizeMode "contain" :flex 1}
                   :source fl/thumb-run}]]
       [:> CardItem
        [:> Body
         [:> Text "ERROR: we couldn't find your current position. This might be due to:"]
         [:> Text {:style {:textAlign "left"}} "\u2022 no gps connection enabled"]
         [:> Text "\u2022 bad signal reception"]]]]]]))


(defn- update-places
  "transact the geocoding result under the user id"
  [data]
  [{:user/id (:user/id data)
    :user/places (:features data)}])

(def autocomplete!
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  (work/pipe #(tool/validate (s/keys :req [::geocoding/query]) % ::invalid-input)
             geocoding/autocomplete!
             #(work/inject % :user/id queries/user-id)
             update-places
             work/transact!))

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
                 :onChangeText #(autocomplete! {::geocoding/query %})}]
      [:> Icon {:name "ios-search"}]]]))

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
    (let [pos    (:coords (:user/position goal))
          lnglat [(:longitude pos) (:latitude pos)]]
      {::directions/coordinates [lnglat
                                 (:coordinates (:geometry goal))]})))

(defn- set-path
  [path]
  [{:user/id (:user/id path)
    :user/directions (dissoc path :user/id)}])

(def update-path! (work/pipe #(work/inject % :user/position queries/user-position)
                             prepare-path
                             directions/request!
                             #(work/inject % :user/id queries/user-id)
                             set-path
                             work/transact!))

(def update-places! (work/pipe #(work/inject % :user/id queries/user-id)
                               set-goal
                               work/transact!))

(defn update-map!
  [target]
  (work/go-try
    (work/<? (update-path! target))
    (work/<? (update-places! target))
    (catch :default error
      (fl/toast! (ex-message error)))))

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

;(go (async/<! (autocomplete! {::geocoding/query "Cartagena, Colombia"
;                              ::geocoding/mode  "mapbox.places"]]])])

;(rework/q queries/user-position)
