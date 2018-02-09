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

(defn- validate-path
  "takes a mapbox directions response and returns it with :route
   as namespaced keys instead of its own.
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

(defn choose-path
  "assocs the path to :user/directions and remove all other temporary paths"
  [path]
  (let [id      (:route/uuid path)
        garbage (remove #{id} (:route/remove path))]
    (concat (map #(vector :db.fn/retractEntity [:route/uuid %]) garbage)
            [{:user/id         (:user/id path)
              :user/directions [:route/uuid id]}])))

(def set-route!
  "takes a mapbox directions object and assocs the user/directions with
  it. All other temporary paths are removed"
  (work/pipe (work/inject :user/id queries/user-id)
             (work/inject :route/remove queries/routes-ids)
             choose-path
             work/transact!))

(defn- next-path!
  "get the next route in memory or fetch one otherwise"
  [routes i goal]
  (swap! i inc)
  (when (nil? (get @routes (inc @i)))
    (go-try (work/transact! (<? (fetch-path! @goal))))))

(defn route-details
  [props i]
  (let [routes       (work/q! queries/routes-ids)
        path         (work/entity [:route/uuid (get @routes @i)])
        goal         (work/q! queries/user-goal)
        route        (first (:route/routes path))
        instructions (sequence (comp (mapcat :steps)
                                     (map :maneuver)
                                     (map :instruction)
                                     (map-indexed vector))
                               (:legs route))]
    (if (nil? path)
      [:> base/Spinner]
      [:> base/Card
       [:> base/CardItem [:> base/Icon {:name "flag"}]
        [:> base/Text (str "distance: " (:distance route) " meters")]]
       [:> base/CardItem [:> base/Icon {:name "flag"}]
        [:> base/Text (str "UUID: " (:route/uuid path) " meters")]]
       [:> base/CardItem [:> base/Icon {:name "information-circle"}]
        [:> base/Text "duration: " (Math/round (/ (:duration route) 60)) " minutes"]]
       [:> base/CardItem [:> base/Icon {:name "time"}]
        [:> base/Text (str "time of arrival: " (js/Date. (+ (js/Date.now)
                                                            (* 1000 (:duration route))))
                           " minutes")]]
       [:> react/View {:style {:flexDirection "row" :justifyContent "space-around"
                               :flex 1}}
        (when (> @i 0)
          [:> base/Button {:warning true :bordered false
                           :on-press #(swap! i dec)}
            [:> base/Icon {:name "ios-arrow-back"}]
            [:> base/Text "previous"]])
        [:> base/Button {:success true :bordered false
                         :on-press #(do (set-route! (into {} path))
                                        ((:goBack (:navigation props))))}
          [:> base/Text "OK"]]
        [:> base/Button {:warning true :iconRight true :bordered false
                         :on-press #(next-path! routes i goal)}
          [:> base/Text "next"]
          [:> base/Icon {:name "ios-arrow-forward"}]]]
       [:> base/CardItem [:> base/Icon {:name "map"}]
        [:> base/Text "Instructions: "]]
       (for [[id text] instructions]
         ^{:key id}
         [:> base/CardItem
          (if (= id (first (last instructions)))
            [:> base/Icon {:name "flag"}]
            [:> base/Icon {:name "ios-navigate-outline"}])
          [:> base/Text text]])])))

(defn instructions
  "basic navigation directions"
  [props]
  (let [counter (r/atom 0)]
    (fn []
      [:> base/Container
       [:> base/Content
        [route-details props counter]]])))

;(work/q queries/routes-ids)