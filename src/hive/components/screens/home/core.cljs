(ns hive.components.screens.home.core
  (:require [hive.components.foreigns.expo :as expo]
            [cljs-react-navigation.reagent :as rn-nav]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [hive.components.screens.home.route :as route]
            [hive.components.screens.errors :as errors]
            [hive.services.mapbox :as mapbox]
            [hive.components.foreigns.react :as react]
            [hive.foreigns :as fl]
            [oops.core :as oops]
            [hive.libs.geometry :as geometry]
            [hive.services.raw.http :as http]
            [hive.services.kamal :as kamal]
            [cljs.core.async :as async]
            [hive.components.symbols :as symbols]
            [datascript.core :as data])
  (:import (goog.date DateTime)))

; NOTE: this is the way to remove all routes ... not sure where to do this
;(for [r (data/q queries/routes-ids (work/db))]
;  [:db.fn/retractEntity [:route/uuid r]])

(defn- set-target
  "associates a target and a path to get there with the user"
  [target props]
  (let [user     (:user/id props)
        navigate (:navigate (:navigation props))
        start    (:coordinates (:geometry (:user/position props)))
        end      (:coordinates (:geometry target))
        now      (new DateTime)
        [url opts] (kamal/directions [start end] now)]
    [[{:user/id user :user/goal target}]
     (delay (.. (js/fetch url (clj->js opts))
                (then #(.json %))
                (then #(route/process-directions % user now))))
     (delay (.. fl/ReactNative (Keyboard.dismiss)))
     [navigate "directions"]]))

(defn- Places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props]
  (let [height   (* 80 (count (:user/places props)))]
    [:> react/View {:height height :paddingTop 100 :paddingLeft 10}
     (for [target (:user/places props)
           :let [distance (/ (geometry/haversine (:user/position props) target)
                             1000)]]
       ^{:key (:id target)}
       [:> react/TouchableOpacity
         {:style {:flex 1 :flexDirection "row"}
          :on-press #(run! work/transact! (set-target target props))}
         [:> react/View {:flex 0.2 :alignItems "center" :justifyContent "flex-end"}
           [:> expo/Ionicons {:name "ios-pin" :size 26 :color "red"}]
           [:> react/Text {:note true} (str (-> distance (.toPrecision 2)) " km")]]
         [:> react/View {:flex 0.8 :justifyContent "flex-end"}
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
  (when (not (empty? text))
    (let [navigate (:navigate (:navigation props))
          args {:query        text
                :proximity    (:user/position props)
                :access_token (:ENV/MAPBOX props)
                :bbox         (:city/bbox (:user/city props))}
          validated (tool/validate ::mapbox/request args ::invalid-input)]
      (if (tool/error? validated)
        [[navigate "location-error" validated]
         (delay (oops/ocall fl/ReactNative "Keyboard.dismiss"))]
        (let [url (mapbox/geocoding args)]
          [(delay (.. (js/fetch url)
                      (then #(.json %))
                      (then tool/keywordize)
                      (then #(assoc % :user/id (:user/id props)))
                      (then update-places)))])))))

(defn- SearchBar
  [props]
  (let [places   (:user/places props)
        token    (data/q queries/mapbox-token (work/db))
        data     (assoc props :ENV/MAPBOX token)
        ref      (volatile! nil)]
    [:> react/View {:flex 1 :flexDirection "row" :backgroundColor "white"
                    :elevation 5 :borderRadius 5 :shadowColor "#000000"
                    :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                    :shadowOpacity 1.0}
     [:> react/View {:height 30 :width 30 :padding 8 :flex 0.1}
       (if (empty? places)
         [:> expo/Ionicons {:name "ios-search" :size 26}]
         [:> react/TouchableWithoutFeedback
           {:onPress #(when (some? @ref)
                        (.clear @ref)
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
        id       (data/q queries/user-id (work/db))
        info    @(work/pull! [{:user/city [:city/geometry :city/bbox :city/name]}
                              :user/places
                              :user/position
                              :user/id]
                             [:user/id id])]
    [:> react/View {:flex 1}
      (if (empty? (:user/places info))
        [symbols/CityMap info]
        [Places (merge props info)])
      [:> react/View {:position "absolute" :width "95%" :height 44 :top 35
                      :left "2.5%" :right "2.5%"}
        [SearchBar info]]
      (when (empty? (:user/places info))
        [:> react/View (merge (symbols/circle 52) symbols/shadow
                              {:position "absolute" :bottom 20 :right 20
                               :backgroundColor "#FF5722"})
          [:> react/TouchableOpacity
            {:onPress #(navigate "settings" {:user/id id})}
            [:> expo/Ionicons {:name "md-apps" :size 26 :style {:color "white"}}]]])]))

(def Screen        (rn-nav/stack-screen Home
                     {:title "map"}))
(def LocationError (rn-nav/stack-screen errors/UserLocation
                                        {:title "location-error"}))

;(data/q queries/routes-ids (work/db))
;(work/transact! [[:db.fn/retractEntity [:route/uuid "cjd5qccf5007147p6t4mneh5r"]]])

;(data/pull (work/db) '[*] [:route/uuid "5b44d414-79a7-40bf-bf65-d6b417497baa"])
