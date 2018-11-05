(ns hive.components.screens.home.core
  (:require [reagent.core :as r]
            [react-native :as React]
            [expo :as Expo]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [hive.services.mapbox :as mapbox]
            [hive.services.location :as location]
            [hive.libs.geometry :as geometry]
            [hive.services.kamal :as kamal]
            [hive.components.symbols :as symbols]
            [datascript.core :as data]
            [hive.state :as state]
            [hive.assets :as assets]))

; NOTE: this is the way to remove all routes ... not sure where to do this
;(for [r (data/q queries/routes-ids (work/db))]
;  [:db.fn/retractEntity [:route/uuid r]])

(defn- set-target
  "associates a target and a path to get there with the user"
  [db navigate target]
  (let [user     (data/q queries/user-id db)
        position (data/pull db [:user/position] [:user/uid user])
        start    (:coordinates (:geometry (:user/position position)))
        end      (:coordinates (:place/geometry target))]
    [{:user/uid  user
      :user/goal [:place/id (:place/id target)]}
     [kamal/directions! [start end] user]
     [React/Keyboard.dismiss]
     [navigate "directions"]]))

(defn- humanize-distance
  "Convert a distance (meters) to human readable form."
  [distance]
  (if (> distance 1000)
    (str (. (/ distance 1000) (toFixed 1)) " km")
    (str (. distance (toFixed 0)) " m")))

(defn- Places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props]
  (let [navigate  (:navigate (:navigation props))
        places   @(work/q! '[:find [(pull ?id [:place/id :place/geometry
                                               :place/text :place/context]) ...]
                             :where [?id :place/id]])
        position @(work/q! queries/user-position)
        height    (* 80 (count places))]
    [:> React/View {:height height :paddingTop 100 :paddingLeft 10}
     (for [target places
           :let [distance (geometry/haversine (:coordinates (:geometry position))
                                              (:coordinates (:place/geometry target)))]]
       ^{:key (:place/id target)}
       [:> React/TouchableOpacity
         {:style    {:flex 1 :flexDirection "row"}
          :onPress #(work/transact! (set-target (work/db) navigate target))}
         [:> React/View {:flex 0.2 :alignItems "center" :justifyContent "flex-end"}
           [:> assets/Ionicons {:name "ios-pin" :size 26 :color "red"}]
           [:> React/Text (humanize-distance distance)]]
         [:> React/View {:flex 0.8 :justifyContent "flex-end"}
           [:> React/Text {:numberOfLines 1} (:place/text target)]
           [:> React/Text {:note true :style {:color "gray"} :numberOfLines 1}
            (str/join ", " (map :text (:place/context target)))]]])]))

(defn- reset-places
  "transact the geocoding result under the user id"
  ([db]
   (for [id (data/q '[:find [?id ...] :where [?id :place/id]]
                    db)]
     [:db.fn/retractEntity id]))
  ([db data]
   (concat (reset-places db)
           (for [f (:features data)]
             (tool/with-ns "place" f)))))

(defn- autocomplete
  "request an autocomplete geocoding result from mapbox and adds its result to the
   app state"
  [text db props]
  (when (not (empty? text))
    (let [navigate (:navigate (:navigation props))
          user     (data/q queries/user-id db)
          data     (data/pull db [:user/position {:user/city [:city/bbox]}]
                                 [:user/uid user])
          args {:query        text
                :proximity    (:user/position data)
                :access_token (:ENV/MAPBOX state/tokens)
                :bbox         (:city/bbox (:user/city data))}
          validated (tool/validate ::mapbox/request args ::invalid-input)]
      (if (tool/error? validated)
        [[navigate "location-error" validated]
         [React/Keyboard.dismiss]]
        [(delay (.. (mapbox/geocoding! args)
                    (then #(reset-places (work/db) %))))]))))

(defn- SearchBar
  [props]
  (let [pids @(work/q! queries/places-id)
        ref   (volatile! nil)]
    [:> React/View {:flex 1 :flexDirection "row" :backgroundColor "white"
                    :elevation 5 :borderRadius 5 :shadowColor "#000000"
                    :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                    :shadowOpacity 1.0}
     [:> React/View {:height 30 :width 30 :padding 8 :flex 0.1}
       (if (empty? pids)
         [:> assets/Ionicons {:name "ios-search" :size 26}]
         [:> React/TouchableWithoutFeedback
           {:onPress #(when (some? @ref)
                        (. @ref clear)
                        (work/transact! (reset-places (work/db))))}
           [:> assets/Ionicons {:name "ios-close-circle" :size 26}]])]
     [:> React/TextInput {:placeholder "Where would you like to go?"
                          :ref #(vreset! ref %) :style {:flex 0.9}
                          :underlineColorAndroid "transparent"
                          :onChangeText #(work/transact! (autocomplete % (work/db) props))}]]))

(defn Home
  "The main screen of the app. Contains a search bar and a mapview"
  [props]
  (r/with-let [callback #(work/transact! (location/set-location (work/db) %))
               tracker  (location/watch! (location/defaults callback))
               navigate (:navigate (:navigation props))
               pids     (work/q! queries/places-id)]
    [:> React/View {:flex 1}
      (if (empty? @pids)
        [symbols/CityMap]
        [Places props])
      [:> React/View {:position "absolute" :width "95%" :height 44 :top 35
                      :left "2.5%" :right "2.5%"}
        [SearchBar props]]
      (when (empty? @pids)
        [:> React/View (merge (symbols/circle 52) symbols/shadow
                              {:position "absolute" :bottom 20 :right 20
                               :backgroundColor "#FF5722"})
          [:> React/TouchableOpacity {:onPress #(navigate "settings")}
            [:> assets/Ionicons {:name "md-apps" :size 26
                                 :style {:color "white"}}]]])]
    ;; remove tracker on component will unmount
    (finally (. tracker (then #(. % remove))))))

;(work/transact! [[:db.fn/retractEntity [:route/uuid "cjd5qccf5007147p6t4mneh5r"]]])
;(data/pull (work/db) '[*] [:route/uuid "5b44dbb7-ac02-40a0-b50f-6c855c5bff14"])

;(data/q '[:find [(pull ?id [*]) ...] :where [?id :place/id]] (work/db))
