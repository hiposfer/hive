(ns hive.components.screens.home.route
  (:require [reagent.core :as r]
            [hive.queries :as queries]
            [hive.components.foreigns.react :as react]
            [hive.services.directions :as directions]
            [hive.libs.time :as local]
            [hive.rework.util :as tool]
            [datascript.core :as data]
            [clojure.core.async :as async]
            [hive.rework.core :as work]
            [cljs-react-navigation.reagent :as rn-nav]))

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

(defn get-path
  "takes a geocoded feature (target) and queries the path to get there
  from the current user position. Returns a transaction or error"
  [goal]
  (let [position (work/q queries/user-position)]
    (if (nil? position)
      (ex-info "missing user location" goal ::user-position-unknown)
      (let [args       {:coordinates [(:coordinates (:geometry position))
                                      (:coordinates (:geometry goal))]
                        :departure (local/date-time)
                        :steps true}
            [url opts] (directions/request args)]
        [url opts (comp (map tool/keywordize)
                        (map reform-path)
                        (map vector))]))))

(defn set-route
  "takes a mapbox directions object and assocs the user/directions with
  it. All other temporary paths are removed"
  [epath user routes]
  (let [path (into {:user/id user :route/remove routes} epath)
        uuid (:route/uuid path)
        garbage (remove #{uuid} (:route/remove path))]
    (concat (map #(vector :db.fn/retractEntity [:route/uuid %]) garbage)
            [{:user/id         (:user/id path)
              :user/directions [:route/uuid uuid]}])))

(defn Instructions
  "basic navigation directions"
  [props]
  (let [counter (r/atom 0)]
    (fn []
      [:> react/View {:style {:flex 1 :alignItems "center" :justifyContent "center"}}
        [:> react/ActivityIndicator {:size "large" :color "#0000ff"}]])))
        ;[route-details props counter]])))

(def Screen    (rn-nav/stack-screen Instructions
                                    {:title "directions"}))

;hive.rework.state/conn
;(work/q queries/routes-ids)
