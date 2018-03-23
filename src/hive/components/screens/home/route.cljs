(ns hive.components.screens.home.route
  (:require [reagent.core :as r]
            [hive.queries :as queries]
            [hive.components.native-base :as base]
            [hive.components.react :as react]
            [hive.services.directions :as directions]
            [hive.rework.util :as tool]
            [datascript.core :as data]
            [hive.services.raw.http :as http]
            [clojure.core.async :as async]
            [hive.rework.core :as work]))

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

(defn- reform-path
  "takes a mapbox directions response and returns it.
   Return an exception if no path was found"
  [path]
  (cond
    (not= (:code path) "Ok")
    (ex-info (or (:msg path) "invalid response")
             path ::invalid-response)

    (not (contains? path :uuid))
    (recur (assoc path :uuid (data/squuid)))

    :ok (tool/with-ns :route path)))

(defn get-path!
  "takes a geocoded feature (target) and queries the path to get there
  from the current user position. Returns a transaction or error"
  [goal]
  (let [loc  (work/q queries/user-position)
        tok  (work/q queries/mapbox-token)]
    (if (nil? loc)
      (async/to-chan [(ex-info "missing user location" goal ::user-position-unknown)])
      (let [args {::directions/coordinates [(:coordinates (:geometry loc))
                                            (:coordinates (:geometry goal))]
                  ::directions/access_token tok}
            url  (directions/request args)]
        (http/json! [url] (comp (map tool/keywordize)
                                (map reform-path)
                                (map vector)))))))

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
       [:> base/Icon {:name "ios-arrow-back"}]
       [:> base/Text "previous"]]
      [:> base/Button {:success true :iconRight true :bordered false
                       :on-press #(swap! i inc)}
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
        [:> base/Text text]])]))

(defn instructions
  "basic navigation directions"
  [props]
  (let [routes  (work/q! queries/routes-ids)
        goal    (work/q! queries/user-goal)
        counter (r/atom 0)]
    (when (nil? (get @routes (inc @counter)))
      (work/transact-chan (get-path! @goal)))
    [:> base/Container
     [:> base/Content
      [route-details props routes counter]]]))

;(work/q queries/routes-ids)

;hive.rework.state/conn
