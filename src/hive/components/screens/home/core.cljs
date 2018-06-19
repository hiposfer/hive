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
            [cljs.core.async :as async]
            [hive.components.symbols :as symbols]))

(defn- choose-route
  "associates a target and a path to get there with the user"
  [target props]
  (let [navigate (:navigate (:navigation props))
        places [{:user/id (:user/id props)
                 :user/goal target}
                [:db.fn/retractAttribute [:user/id (:user/id props)]
                                         :user/places]]
        garbage (map #(vector :db.fn/retractEntity [:route/uuid %])
                      (work/q queries/routes-ids))]
    [(concat places garbage)
     [http/json! (route/get-path target)]
     (delay (oops/ocall fl/ReactNative "Keyboard.dismiss"))
     [navigate "directions"]]))

(defn- Places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props]
  (let [height   (* 80 (count (:user/places props)))]
    [:> react/View {:style {:height height :paddingTop 100 :paddingLeft 10}}
     (for [target (:user/places props)
           :let [distance (/ (geometry/haversine (:user/position props) target)
                             1000)]]
       ^{:key (:id target)}
       [:> react/TouchableOpacity
         {:style {:flex 1 :flexDirection "row"}
          :on-press #(run! work/transact! (choose-route target props))}
         [:> react/View {:style {:flex 0.2 :alignItems "center" :justifyContent "flex-end"}}
           [:> expo/Ionicons {:name "ios-pin" :size 26 :color "red"}]
           [:> react/Text {:note true} (str (-> distance (.toPrecision 2)) " km")]]
         [:> react/View {:style {:flex 0.8 :justifyContent "flex-end"}}
           [:> react/Text {:numberOfLines 1} (:text target)]
           [:> react/Text {:note true :style {:color "gray"} :numberOfLines 1}
             (str/join ", " (map :text (:context target)))]]])]))

(defn- update-places
  "transact the geocoding result under the user id"
  [data]
  [{:user/id (:user/id data)
    :user/places (or (:features data) [])}])

(defn- autocomplete
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  [text props]
  (if (empty? text)
    (update-places nil)
    (let [args {::geocoding/query text
                ::geocoding/proximity (:user/position props)
                ::geocoding/access_token (:ENV/MAPBOX props)
                ::geocoding/bbox (:city/bbox (:user/city props))}
          validated (tool/validate ::geocoding/request args ::invalid-input)
          navigate (:navigate (:navigation props))]
      (if (tool/error? validated)
        [[navigate "location-error" validated]
         (delay (oops/ocall fl/ReactNative "Keyboard.dismiss"))]
        (let [args (geocoding/defaults validated)
              url  (geocoding/autocomplete args)
              xform (comp (map tool/keywordize)
                          (map #(assoc % :user/id (:user/id props)))
                          (map update-places))]
          [[http/json! url {} xform]])))))

(defn- SearchBar
  [props places]
  (let [data     (work/inject props :ENV/MAPBOX queries/mapbox-token)
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
                      :ref #(vreset! ref %) :style {:flex 0.9}
                      :underlineColorAndroid "transparent"
                      :onChangeText #(run! work/transact! (autocomplete % data))}]]))


(defn Home
  "The main screen of the app. Contains a search bar and a mapview"
  [props]
  (let [navigate (:navigate (:navigation props))
        id       (work/q queries/user-id)
        info    @(work/pull! [:user/places :user/goal :user/position
                              {:user/city [:city/geometry :city/bbox :city/name]}
                              {:user/directions [:route/routes]}]
                             [:user/id id])]
    [:> react/View {:style {:flex 1}}
      (if (empty? (:user/places info))
        [symbols/CityMap info]
        [Places (merge props info {:user/id id})])
      [:> react/View {:style {:position "absolute" :width "95%" :height 44 :top 35
                              :left "2.5%" :right "2.5%"}}
        [SearchBar (merge info {:user/id id})
                   (:user/places info)]]
      (when (empty? (:user/places info))
        [:> react/View {:style (merge (symbols/circle 52) symbols/shadow
                                      {:position "absolute" :bottom 20 :right 20}
                                      {:backgroundColor "#FF5722"})}
          [:> react/TouchableOpacity
            {:onPress #(navigate "settings" {:user/id id})}
            [:> expo/Ionicons {:name "md-apps" :size 26 :style {:color "white"}}]]])]))

(def Screen        (rn-nav/stack-screen Home
                     {:title "map"}))
(def LocationError (rn-nav/stack-screen errors/UserLocation
                                        {:title "location-error"}))

;(work/q queries/routes-ids)
;(work/transact! [[:db.fn/retractEntity [:route/uuid "cjd5qccf5007147p6t4mneh5r"]]])

;(work/pull '[*] [:route/uuid "cjd5rx3pn00qj47p6lc1z7n1v"])
