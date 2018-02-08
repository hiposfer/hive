(ns hive.components.screens.home.route
  (:require [reagent.core :as r]
            [hive.queries :as queries]
            [hive.components.native-base :as base]
            [hive.components.react :as react]
            [hive.rework.core :as work :refer-macros [go-try <?]]
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
             directions/request!))

;(defn- tx-path
;  "takes a mapbox directions response and returns a transaction vector
;  to use with transact!. Return an exception if no path was found"
;  [path]
;  (let [id      (:uuid path)
;        garbage (remove #{id} (:route/remove path))]
;    (concat (map #(vector :db.fn/retractEntity [:route/uuid %]) garbage)
;            [(tool/with-ns :route (dissoc path :user/id))
;             {:user/id (:user/id path)
;              :user/directions [:route/uuid id]}])))

(defn- validate-path
  "takes a mapbox directions response and returns it.
   Return an exception if no path was found"
  [path]
  (cond
    (not= (:code path) "Ok")
    (ex-info (or (:msg path) "invalid response")
             path ::invalid-response)

    (not (contains? path :uuid))
    (recur (assoc path :uuid (data/squuid)))

    :ok [(tool/with-ns :route path)]))

(def fetch-path!
  "takes a geocoded feature (target) and queries the path to get there.
  Returns a transaction vector to use with transact!"
  (work/pipe get-path!
             validate-path))

(defn route-details
  [props routes i]
  (println)
  (let [directions   (work/entity [:route/uuid (get @routes @i)])
        route        (first (:route/routes directions))
        instructions (sequence (comp (mapcat :steps)
                                     (map :maneuver)
                                     (map :instruction)
                                     (map-indexed vector))
                               (:legs route))]
    [:> base/Card
     [:> base/CardItem [:> base/Icon {:name "flag"}]
      [:> base/Text (str "distance: " (:distance route) " meters")]]
     [:> base/CardItem [:> base/Icon {:name "flag"}]
      [:> base/Text (str "UUID: " (:route/uuid directions) " meters")]]
     [:> base/CardItem [:> base/Icon {:name "information-circle"}]
      [:> base/Text "duration: " (Math/round (/ (:duration route) 60)) " minutes"]]
     [:> base/CardItem [:> base/Icon {:name "time"}]
      [:> base/Text (str "time of arrival: " (js/Date. (+ (js/Date.now)
                                                          (* 1000 (:duration route))))
                         " minutes")]]
     [:> react/View {:style {:flexDirection "row" :alignItems "flex-start"
                             :flex 1}}
      [:> base/Button {:danger true :bordered false
                       :on-press #(swap! i dec)}
       [:> base/Text "previous"]
       [:> base/Icon {:name "ion-arrow-forward"}]]
      [:> base/Button {:success true :iconRight true :bordered false
                       :on-press #(swap! i inc)}
       [:> base/Text "next"]
       [:> base/Icon {:name "ion-arrow-back"}]]]
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
  (let [routes  (work/q! queries/routes-ids)
        goal    (work/q! queries/user-goal)
        counter (r/atom 0)]
    (when (nil? (get @routes (inc @counter)))
      (go-try (work/transact! (<? (fetch-path! @goal)))))
    (fn []
      [:> base/Container
       [:> react/View
        [route-details props routes counter]]])))

;(work/q queries/routes-ids)
