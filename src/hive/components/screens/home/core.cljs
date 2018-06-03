(ns hive.components.screens.home.core
  (:require [hive.components.foreigns.expo :as expo]
            [cljs-react-navigation.reagent :as rn-nav]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [hive.components.screens.home.route :as route]
            [hive.components.screens.errors :as errors]
            [hive.services.geocoding :as geocoding]
            [hive.components.foreigns.react :as react]
            [hive.foreigns :as fl]
            [oops.core :as oops]
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
    :user/places (or (:features data) [])}])

(defn choose-route
  "associates a target and a path to get there with the user"
  [target props]
  (let [places [{:user/id props
                 :user/goal target}
                [:db.fn/retractAttribute [:user/id props] :user/places]]
        garbage (map #(vector :db.fn/retractEntity [:route/uuid %])
                      (work/q queries/routes-ids))]
    (concat places garbage)))

(defn places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props]
  (let [navigate (:navigate (:navigation props))
        height   (* 80 (count (:user/places props)))]
    [:> react/View {:style {:height height :paddingTop 100 :paddingLeft 10}}
     (for [target (:user/places props)
           :let [distance (/ (geometry/haversine (:user/position props) target)
                             1000)]]
       ^{:key (:id target)}
       [:> react/TouchableOpacity
         {:style {:flex 1 :flexDirection "row"}
          :on-press #(do (work/transact!
                           (async/onto-chan (http/json! (route/get-path target))
                                            (choose-route target props)))
                         (oops/ocall fl/ReactNative "Keyboard.dismiss")
                         (navigate "directions"))}
         [:> react/View {:style {:flex 0.2 :alignItems "center" :justifyContent "flex-end"}}
           [:> expo/Ionicons {:name "ios-pin" :size 26 :color "red"}]
           [:> react/Text {:note true} (str (-> distance (.toPrecision 2)) " km")]]
         [:> react/View {:style {:flex 0.8 :justifyContent "flex-end"}}
           [:> react/Text {:numberOfLines 1} (:text target)]
           [:> react/Text {:note true :style {:color "gray"} :numberOfLines 1}
             (str/join ", " (map :text (:context target)))]]])]))

(defn autocomplete
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  [text data]
  (if (empty? text)
    (update-places data)
    (let [args {::geocoding/query text
                ::geocoding/proximity (:user/position data)
                ::geocoding/access_token (:token/mapbox data)
                ::geocoding/bbox (:city/bbox (:user/city data))}
          validated (tool/validate ::geocoding/request args ::invalid-input)]
      (if (tool/error? validated)
        [{:user/id (:user/id data)
          :user/places validated}] ;; handle error in UI
        (let [args (geocoding/defaults validated)
              url  (geocoding/autocomplete args)
              xform (comp (map tool/keywordize)
                          (map #(assoc % :user/id (:user/id data)))
                          (map update-places))]
          [http/json! url {} xform])))))
;(go-try
;  (work/transact! (<? (geocode! text)))
;  (catch :default _
;    (try (work/transact! (<? (location/watch! position/defaults)))
;         (catch :default _
;           (.dismiss fl/Keyboard)
;           (navigate "location-error")))))))


(defn- search-bar
  [props places]
  (let [data     (work/inject props :token/mapbox queries/mapbox-token)
        ref      (volatile! nil)]
    [:> react/View {:style {:flex 1 :flexDirection "row" :backgroundColor "white"
                            :elevation 5 :borderRadius 5 :shadowColor "#000000"
                            :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                            :shadowOpacity 1.0}}

     [:> react/View {:style {:height 30 :width 30 :padding 8 :flex 0.1}}
       (if (empty? places)
         [:> expo/Ionicons {:name "ios-search" :size 26}]
         [:> react/TouchableWithoutFeedback
           {:on-press #(do (when (some? @ref) (.clear @ref))
                           (work/transact! (update-places props)))}
           [:> expo/Ionicons {:name "ios-close-circle" :size 26}]])]
     [:> react/Input {:placeholder "Where would you like to go?"
                      :ref #(vreset! ref %)
                      :style {:flex 0.9}
                      :onChangeText #(work/transact! (autocomplete % data))}]]))

(defn city-map
  "a React Native MapView component which will only re-render on user-city change"
  [user]
  (let [coords (:coordinates (:city/geometry (:user/city user)))]
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
                              {:user/city [:city/geometry :city/bbox :city/name]}
                              {:user/directions [:route/routes]}]
                             [:user/id id])]
    (if (tool/error? (:user/places info))
      [errors/user-location props]
      [:> react/View {:style {:flex 1}}
        (if (empty? (:user/places info))
          [city-map info]
          [places (merge props info)])
        [:> react/View {:style {:position "absolute" :top 35 :left 20
                                :width 340 :height 42}}
          [search-bar (merge info {:user/id id})
                      (:user/places info)]]
        (when (empty? (:user/places info))
          [:> react/View {:style {:position "absolute" :bottom 20 :right 20
                                  :width 52 :height 52 :borderRadius 52/2
                                  :alignItems "center" :justifyContent "center"
                                  :backgroundColor "#FF5722" :elevation 3
                                  :shadowColor "#000000" :shadowRadius 5
                                  :shadowOffset {:width 0 :height 3} :shadowOpacity 1.0}}
            [:> react/TouchableOpacity
              {:onPress #(navigate "settings" {:user/id id})}
              [:> expo/Ionicons {:name "md-apps" :size 26 :style {:color "white"}}]]])])))


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
