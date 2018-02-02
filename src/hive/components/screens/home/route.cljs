(ns hive.components.screens.home.route
  (:require [reagent.core :as r]
            [hive.queries :as queries]
            [hive.components.native-base :as base]
            [hive.components.react :as react]
            [hive.rework.core :as work]))

;; TODO: https://github.com/GeekyAnts/NativeBase/issues/826
(defn route-details
  [props id]
  (let [route        (first (:route/routes (work/entity [:route/uuid id])))
        instructions (sequence (comp (mapcat :steps)
                                     (map :maneuver)
                                     (map :instruction)
                                     (map-indexed vector))
                               (:legs route))]
    [:> base/Card
     [:> base/CardItem [:> base/Icon {:name "flag"}]
      [:> base/Text (str "distance: " (:distance route) " meters")]]
     [:> base/CardItem [:> base/Icon {:name "information-circle"}]
      [:> base/Text "duration: " (Math/round (/ (:duration route) 60)) " minutes"]]
     [:> base/CardItem [:> base/Icon {:name "time"}]
      [:> base/Text (str "time of arrival: " (js/Date. (+ (js/Date.now)
                                                          (* 1000 (:duration route))))
                         " minutes")]]
     [:> base/CardItem [:> base/Icon {:name "map"}]
      [:> base/Text "Instructions: "]]
     (for [[id text] instructions]
       ^{:key id} [:> base/CardItem
                   (if (= id (first (last instructions)))
                     [:> base/Icon {:name "flag"}]
                     [:> base/Icon {:name "ios-navigate-outline"}])
                   [:> base/Text text]])]))

(defn instructions
  "basic navigation directions"
  [props]
  (let [routes        @(work/q! queries/routes-ids)]
    [:> base/Container
     [:> react/View
      [:> base/DeckSwiper {:dataSource routes
                           :renderItem #(r/as-element (route-details props %))}]]]))
