(ns hive.screens.home.ui
  "The Home screen a.k.a landing page is the first thing
  that the user sees when the app starts.

  It features a full screen map with a search box and a button
  leading to the settings page"
  (:require [reagent.core :as r]
            [react-native :as React]
            [expo :as Expo]
            [clojure.string :as str]
            [hive.queries :as queries]
            [hive.services.location :as location]
            [hive.utils.geometry :as geometry]
            [hive.services.kamal :as kamal]
            [hive.screens.symbols :as symbols]
            [datascript.core :as data]
            [hive.state :as state]
            [hive.assets :as assets]
            [hive.screens.errors :as errors]
            [hive.screens.home.handlers :as handle]))

(defn- Places
  "list of items resulting from a geocode search, displayed to the user to choose his
  destination"
  [props]
  (let [navigate  (:navigate (:navigation props))
        position  @(state/q! queries/user-position)]
    [:> React/View {:flex 1 :paddingTop 100 :paddingLeft 10}
     (for [id @(state/q! queries/places-id)
           :let [target   (data/entity (state/db) id)
                 distance (geometry/haversine (:coordinates (:geometry position))
                                              (:coordinates (:place/geometry target)))]]
       ^{:key (:place/id target)}
       [:> React/TouchableOpacity
         {:style    {:height 60 :flexDirection "row"}
          :onPress #(state/transact! (handle/set-target (state/db) navigate target))}
         [symbols/PointOfInterest
           [:> assets/Ionicons {:name "ios-pin" :size 26 :color "red"}]
           [:> React/Text (handle/humanize-distance distance)]
           [:> React/Text {:numberOfLines 1} (:place/text target)]
           [:> React/Text {:style {:color "gray"} :numberOfLines 1}
                          (str/join ", " (map :text (:place/context target)))]]])]))

(defn- ErrorModal
  [props]
  (let [error-type @(state/q! '[:find ?error-type .
                                :where [?error :error/id :home/search]
                                       [?error :error/type ?error-type]])]
    (when (some? error-type)
      [:> React/Modal {:animationType "slide" :presentationStyle "overFullScreen"
                       :transparent false :visible (some? error-type)
                       :onRequestClose #(state/transact! [[:db/retractEntity [:error/id :home/search]]])}
        (case error-type
          :internet/missing [errors/InternetMissing]
          :location/unknown [errors/LocationUnknown])])))

(defn- SearchBar
  [props]
  (let [pids @(state/q! queries/places-id)
        ref   (volatile! nil)]
    [:> React/View {:flex 1 :flexDirection "row" :backgroundColor "white"
                    :elevation 5 :borderRadius 5 :shadowColor "#000000"
                    :shadowRadius 5 :shadowOffset {:width 0 :height 3}
                    :shadowOpacity 1.0}
      [ErrorModal props]
      [:> React/View {:height 30 :width 30 :padding 8 :flex 0.1}
        (if (empty? pids)
          [:> assets/Ionicons {:name "ios-search" :size 26}]
          [:> React/TouchableWithoutFeedback
            {:onPress #(when (some? @ref)
                         (. @ref clear)
                         (state/transact! (handle/reset-places (state/db))))}
            [:> assets/Ionicons {:name "ios-close-circle" :size 26}]])]
      [:> React/TextInput {:placeholder "Where would you like to go?"
                           :ref #(vreset! ref %) :style {:flex 0.9}
                           :underlineColorAndroid "transparent"
                           :onChangeText #(state/transact! (handle/autocomplete % (state/db)))}]]))

(defn- on-location-updated [position]
  (state/transact! (location/set-location (state/db) position)))

(defn Screen
  "The main screen of the app. Contains a search bar and a mapview"
  [props]
  (r/with-let [tracker  (location/watch! {:callback on-location-updated})
               navigate (:navigate (:navigation props))
               pids     (state/q! queries/places-id)
               bbox     (state/q! queries/user-area-bbox)
               position (state/q! queries/user-position)]
    [:> React/View {:flex 1 :backgroundColor "white"}
      (if (empty? @pids)
        [:> Expo/MapView {:region (geometry/mapview-region {:bbox @bbox
                                                            :position @position})
                          :showsUserLocation     true
                          :style                 {:flex 1}
                          :showsMyLocationButton true}]
        [Places props])
      [:> React/View {:position "absolute" :width "95%" :height 44 :top 35
                      :left "2.5%" :right "2.5%"}
        [SearchBar props]]
      (when (empty? @pids)
        [:> React/View (merge (symbols/circle 52) symbols/shadow
                              {:position "absolute" :bottom 20 :right 20
                               :backgroundColor "#FF5722"})
          [:> React/TouchableOpacity {:onPress #(state/transact! [[navigate "settings"]
                                                                  [kamal/get-areas!]])}
            [:> assets/Ionicons {:name "md-apps" :size 26
                                 :style {:color "white"}}]]])]
    ;; remove tracker on component will unmount
    (finally (. tracker (then #(. % (remove)))))))
