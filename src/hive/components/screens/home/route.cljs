(ns hive.components.screens.home.route
  (:require [reagent.core :as r]
            [hive.queries :as queries]
            [hive.components.native-base :as base]
            [hive.components.react :as react]
            [hive.rework.core :as work]
            [hive.services.directions :as directions]
            [hive.rework.util :as tool]
            [datascript.core :as data]))

(defn- prepare-coordinates
  "transform the user/position attribute to the ::directions/coordinates format"
  [goal]
  (if (nil? (:user/position goal))
    (ex-info "missing user location" goal ::user-position-unknown)
    {::directions/coordinates [(:coordinates (:geometry (:user/position goal)))
                               (:coordinates (:geometry goal))]}))

(def get-path!
  "takes a geocoded feature (target) and queries the path to get there.
  Returns a transaction vector to use with transact!"
  (work/pipe (work/inject :user/position queries/user-position)
             prepare-coordinates
             directions/request!
             (work/inject :user/id queries/user-id)))

(defn- validate-path
  "takes a mapbox directions response and returns a transaction vector
  to use with transact!. Return an exception if no path was found"
  [path]
  (cond
    (not= (:code path) "Ok")
    (ex-info (or (:msg path) "invalid response")
             path ::invalid-response)

    (not (contains? path :uuid))
    (recur (assoc path :uuid (data/squuid)))

    :ok path))

(def next-path!
  "takes a geocoded feature (target) and queries the path to get there.
  Returns a transaction vector to use with transact!"
  (work/pipe get-path!
             validate-path
             #(vector (tool/with-ns :route (dissoc % :user/id)))))

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
       ^{:key id}
       [:> base/CardItem
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

;(work/q queries/routes-ids)