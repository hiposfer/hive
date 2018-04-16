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
  (let [args  {::geocoding/query text
               ::geocoding/proximity (:user/position data)
               ::geocoding/access_token token
               ::geocoding/bbox (:city/bbox (:user/city data))}
        args (tool/validate ::geocoding/request args ::invalid-input)]
    (if (tool/error? args) args
      (let [args (geocoding/defaults args)
            url  (geocoding/autocomplete args)
            xform (comp (map tool/keywordize)
                        (map #(assoc % :user/id id))
                        (map update-places))]
        [url {} xform]))))
    ;(go-try
    ;  (work/transact! (<? (geocode! text)))
    ;  (catch :default _
    ;    (try (work/transact! (<? (location/watch! position/defaults)))
    ;         (catch :default _
    ;           (.dismiss fl/Keyboard)
    ;           (navigate "location-error")))))))

(defn choose-route
  "associates a target and a path to get there with the user"
  [target id]
  (let [places [{:user/id id
                 :user/goal target}
                [:db.fn/retractAttribute [:user/id id] :user/places]]
        garbage (map #(vector :db.fn/retractEntity [:route/uuid %])
                      (work/q queries/routes-ids))]
    (concat places garbage)))

;(async/go
;  (println
;    (async/<!
;      (http/json!
;        (route/get-path {:properties {},
;                         :relevance 1,
;                         :type "Feature",
;                         :geometry {:type "Point", :coordinates [8.681239 50.09051]},
;                         :place_type ["address"],
;                         :center [8.681239 50.09051],
;                         :place_name "Mittlerer Schafhofweg, Frankfurt am Main, Hessen 60598, Germany",
;                         :id "address.1585297524",
;                         :context [{:id "postcode.15081868302516260", :text "60598"}
;                                   {:id "place.7300924236190980",
;                                    :wikidata "Q1794",
;                                    :text "Frankfurt am Main"}
;                                   {:id "region.3907",
;                                    :short_code "DE-HE",
;                                    :wikidata "Q1199",
;                                    :text "Hessen"}
;                                   {:id "country.3135",
;                                    :short_code "de",
;                                    :wikidata "Q183",
;                                    :text "Germany"}],
;                         :text "Mittlerer Schafhofweg"})))))

(defn places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props features]
  (let [position @(work/q! queries/user-position)
        navigate (:navigate (:navigation props))
        id       (work/q queries/user-id)]
    [:> base/List {:icon true :style {:flex 1}}
     (for [target features
           :let [distance (/ (geometry/haversine position target) 1000)]]
       ^{:key (:id target)}
       [:> base/ListItem {:icon     true
                          :on-press #(do (work/transact-chan
                                           (async/onto-chan (http/json! (route/get-path target))
                                                            (choose-route target id)))
                                         (.dismiss fl/Keyboard)
                                         (navigate "directions"))
                          :style    {:height 50 :paddingVertical 30}}
        [:> base/Left
         [:> react/View {:align-items "center"}
          [:> base/Icon {:name "pin"}]
          [:> base/Text {:note true} (str (.toPrecision distance 3) " km")]]]
        [:> base/Body
         [:> base/Text {:numberOfLines 1} (:text target)]
         [:> base/Text {:note true :style {:color "gray"} :numberOfLines 1}
                       (str/join ", " (map :text (:context target)))]]])]))

(defn- search-bar
  [props places]
  (let [id       (:user/id props)
        data     (select-keys props [:user/position :user/city])
        token    (work/q queries/mapbox-token)
        ref      (volatile! nil)]
    [:> react/View {:style {:flex 1 :backgroundColor "white" :elevation 5
                            :borderRadius 5 :justifyContent "center"
                            :shadowColor "#000000" :shadowRadius 5
                            :shadowOffset {:width 0 :height 3} :shadowOpacity 1.0}}
     [:> base/Item
      [:> base/Button {:transparent true :full true
                       :on-press    #(do (.clear @ref)
                                         (work/transact! (clear-places id)))}
        (if (empty? places)
          [:> base/Icon {:name "ios-search"}]
          [:> base/Icon {:name "close"}])]
      [:> base/Input {:placeholder "Where would you like to go?"
                      :ref #(when % (vreset! ref (.-_root %)))
                      :onChangeText #(if (empty? %) (work/transact! (clear-places id))
                                       (let [r (autocomplete % id data token)]
                                         (if (tool/error? r)
                                           (tool/log! (ex-message r))
                                           (work/transact-chan (http/json! r)))))}]]]))

(defn city-map
  "a React Native MapView component which will only re-render on user-city change"
  [user]
  (let [info  @(work/pull! [{:user/city [:city/geometry :city/bbox :city/name]}]
                           user)
        coords (:coordinates (:city/geometry (:user/city info)))]
    [:> expo/MapView {:region (merge (latlng coords)
                                     {:latitudeDelta 0.02 :longitudeDelta 0.02})
                      :showsUserLocation true :style {:flex 1}
                      :showsMyLocationButton true}]))

(defn home
  "the main screen of the app. Contains a search bar and a mapview"
  [props]
  (let [navigate (:navigate (:navigation props))
        id       (work/q queries/user-id)
        info    @(work/pull! [:user/places :user/goal :user/position
                              {:user/directions [:route/routes]}]
                             [:user/id id])]
    [:> react/View {:style {:flex 1}}
      [city-map [:user/id id]]
      [:> react/View {:style {:position "absolute" :top 35 :left 20 :width 340 :height 42}}
       [search-bar (merge info {:user/id id})
                   (:user/places info)]]
      [:> react/View {:style {:position "absolute" :bottom 20 :right 20
                              :width 52 :height 52 :borderRadius 52/2
                              :alignItems "center" :justifyContent "center"
                              :backgroundColor "#FF5722" :elevation 3
                              :shadowColor "#000000" :shadowRadius 5
                              :shadowOffset {:width 0 :height 3} :shadowOpacity 1.0}}
       [:> base/Button {:transparent true :full true
                        :onPress #(navigate "settings" {:user/id id})}
         [:> base/Icon {:name "md-apps" :style {:color "white"}}]]]]))
       ;[:> react/View {:style {:backgroundColor "red" :width 30 :height 30}}]]]))

      ;[search-bar props (:user/places info)]]]
      ;(if (not-empty (:user/places info))
      ;[places props (:user/places info)]
      ;[:> react/View {:style {:flex 1}}

;(when (some? (:user/goal info))
    ;  [:> base/Button {:full true
    ;                   :on-press #((:navigate (:navigation props)) "directions")}
    ;   [:> base/Icon {:name "information-circle" :transparent true}]
    ;   [:> base/Text {:numberOfLines 1} (:text (:user/goal info))]])))

(def Directions    (rn-nav/stack-screen route/instructions
                     {:title "directions"}))
(def Screen        (rn-nav/stack-screen home
                     {:title "map"}))
(def LocationError (rn-nav/stack-screen errors/user-location
                     {:title "location-error"}))

;(work/q queries/routes-ids)
;(work/transact! [[:db.fn/retractEntity [:route/uuid "cjd5qccf5007147p6t4mneh5r"]]])

;(work/pull '[*] [:route/uuid "cjd5rx3pn00qj47p6lc1z7n1v"])
