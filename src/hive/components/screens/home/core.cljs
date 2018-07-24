(ns hive.components.screens.home.core
  (:require [reagent.core :as r]
            [hive.components.foreigns.expo :as expo]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [hive.components.screens.home.route :as route]
            [hive.services.mapbox :as mapbox]
            [hive.services.location :as location]
            [hive.components.foreigns.react :as react]
            [hive.foreigns :as fl]
            [hive.libs.geometry :as geometry]
            [hive.services.kamal :as kamal]
            [hive.components.symbols :as symbols]
            [datascript.core :as data])
  (:import (goog.date DateTime)))

; NOTE: this is the way to remove all routes ... not sure where to do this
;(for [r (data/q queries/routes-ids (work/db))]
;  [:db.fn/retractEntity [:route/uuid r]])

(defn- set-target
  "associates a target and a path to get there with the user"
  [db navigate target]
  (let [user     (data/q queries/user-id db)
        position (data/pull db [:user/position] [:user/id user])
        start    (:coordinates (:geometry (:user/position position)))
        end      (:coordinates (:geometry target))]
    [[{:user/id user :user/goal target}]
     (delay (.. (kamal/directions! [start end] (new DateTime))
                (then #(route/process-directions % user))))
                ;; TODO: error handling
     (delay (.. fl/ReactNative (Keyboard.dismiss)))
     [navigate "directions"]]))

(defn- Places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [user props]
  (let [navigate (:navigate (:navigation props))
        data    @(work/pull! [:user/places :user/position]
                             [:user/id user])
        height   (* 80 (count (:user/places data)))]
    [:> react/View {:height height :paddingTop 100 :paddingLeft 10}
     (for [target (:user/places data)
           :let [distance (/ (geometry/haversine (:user/position data) target)
                             1000)]]
       ^{:key (:id target)}
       [:> react/TouchableOpacity
         {:style    {:flex 1 :flexDirection "row"}
          :on-press #(run! work/transact! (set-target (work/db) navigate target))}
         [:> react/View {:flex 0.2 :alignItems "center" :justifyContent "flex-end"}
           [:> expo/Ionicons {:name "ios-pin" :size 26 :color "red"}]
           [:> react/Text {:note true} (str (. distance (toPrecision 2)) " km")]]
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
  [text db props]
  (when (not (empty? text))
    (let [navigate (:navigate (:navigation props))
          user     (data/q queries/user-id db)
          data     (data/pull db [:user/position {:user/city [:city/bbox]}]
                                 [:user/id user])
          token    (data/q queries/mapbox-token db)
          args {:query        text
                :proximity    (:user/position data)
                :access_token token
                :bbox         (:city/bbox (:user/city data))}
          validated (tool/validate ::mapbox/request args ::invalid-input)]
      (if (tool/error? validated)
        [[navigate "location-error" validated]
         (delay (.. fl/ReactNative (Keyboard.dismiss)))]
        [(delay (.. (mapbox/geocoding! args)
                    (then #(assoc % :user/id user))
                    (then update-places)))]))))

(defn- SearchBar
  [user props]
  (let [data    @(work/pull! [:user/places :user/id]
                             [:user/id user])
        ref      (volatile! nil)]
    [:> react/View {:flex 1 :flexDirection "row" :backgroundColor "white"
                    :elevation 5 :borderRadius 5 :shadowColor "#000000"
                    :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                    :shadowOpacity 1.0}
     [:> react/View {:height 30 :width 30 :padding 8 :flex 0.1}
       (if (empty? (:user/places data))
         [:> expo/Ionicons {:name "ios-search" :size 26}]
         [:> react/TouchableWithoutFeedback
           {:onPress #(when (some? @ref)
                        (. @ref clear)
                        (work/transact! (update-places data)))}
           [:> expo/Ionicons {:name "ios-close-circle" :size 26}]])]
     [:> react/Input {:placeholder "Where would you like to go?"
                      :ref #(vreset! ref %) :style {:flex 0.9}
                      :underlineColorAndroid "transparent"
                      :onChangeText #(run! work/transact! (autocomplete % (work/db) props))}]]))

(defn Home
  "The main screen of the app. Contains a search bar and a mapview"
  [props]
  (r/with-let [db       (work/db)
               tracker  (location/watch! (location/with-defaults db))
               navigate (:navigate (:navigation props))
               id       (data/q queries/user-id db)
               info     (work/pull! [:user/places] [:user/id id])]
    [:> react/View {:flex 1}
      (if (empty? (:user/places @info))
        [symbols/CityMap]
        [Places id props])
      [:> react/View {:position "absolute" :width "95%" :height 44 :top 35
                      :left "2.5%" :right "2.5%"}
        [SearchBar id props]]
      (when (empty? (:user/places @info))
        [:> react/View (merge (symbols/circle 52) symbols/shadow
                              {:position "absolute" :bottom 20 :right 20
                               :backgroundColor "#FF5722"})
          [:> react/TouchableOpacity
            {:onPress #(navigate "settings" {:user/id id})}
            [:> expo/Ionicons {:name "md-apps" :size 26 :style {:color "white"}}]]])]
    ;; remove tracker on component will unmount
    (finally (. tracker (then #(. % remove))))))

;(data/q queries/routes-ids (work/db))
;(work/transact! [[:db.fn/retractEntity [:route/uuid "cjd5qccf5007147p6t4mneh5r"]]])

;(data/pull (work/db) '[*] [:route/uuid "5b44dbb7-ac02-40a0-b50f-6c855c5bff14"])
