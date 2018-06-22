(ns hive.components.screens.home.route
  (:require [hive.queries :as queries]
            [hive.components.foreigns.react :as react]
            [hive.services.directions :as directions]
            [hive.rework.util :as tool]
            [datascript.core :as data]
            [hive.rework.core :as work]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.components.symbols :as symbols]
            [hive.foreigns :as fl]
            [hive.components.foreigns.expo :as expo]
            [hive.libs.geometry :as geometry]
            [goog.date.duration :as duration]
            [clojure.string :as str])
  (:import (goog.date DateTime)))

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

(defn- local-time
  "returns a compatible Java LocalDateTime string representation"
  []
  (let [now   (new DateTime)
        gtime (-> now (.toIsoString true))]
    (str/replace gtime " " "T")))

(defn- reform-path
  "takes a mapbox directions response and returns it.
   Return an exception if no path was found"
  [path user]
  (let [uid  (data/squuid)
        path (->> (assoc path :uuid uid)
                  (tool/with-ns :route))]
    [path {:user/id user
           :user/route [:route/uuid uid]}]))

(defn get-path
  "takes a geocoded feature (target) and queries the path to get there
  from the current user position. Returns a transaction or error"
  [data goal]
  (let [position (:user/position data)
        user     (:user/id data)]
    (if (nil? position)
      (ex-info "missing user location" goal ::user-position-unknown)
      (let [args {:coordinates [[8.645333, 50.087314]
                                [8.635897, 50.104172]]
                  :departure "2018-05-07T10:15:30"
                  :steps true}
            [url opts] (directions/request args)]
        [url opts (map tool/keywordize)
                  (halt-when #(not= "Ok" (:code %)))
                  (map #(reform-path % user))]))))

(defn set-route
  "takes a mapbox directions object and assocs the user/directions with
  it. All other temporary paths are removed"
  [epath user routes]
  (let [path (into {:user/id user :route/remove routes} epath)
        uuid (:route/uuid path)
        garbage (remove #{uuid} (:route/remove path))]
    (concat (map #(vector :db.fn/retractEntity [:route/uuid %]) garbage)
            [{:user/id    (:user/id path)
              :user/route [:route/uuid uuid]}])))

(defn- Transfers
  [window]
  (into [:> react/View {:flex 1 :flexDirection "row" :justifyContent "space-around"
                         :top "2.5%"}]
        [[:> expo/Ionicons {:name "ios-walk" :size 32}]
         [:> expo/Ionicons {:name "ios-arrow-forward" :size 26 :color "gray"}]
         [:> expo/Ionicons {:name "ios-bus" :size 32}]
         [:> expo/Ionicons {:name "ios-arrow-forward" :size 26 :color "gray"}]
         [:> expo/Ionicons {:name "ios-train" :size 32}]]))

(defn Instructions
  "basic navigation directions"
  [props]
  (let [window  (tool/keywordize (.. fl/ReactNative (Dimensions/get "window")))
        id      (work/q queries/user-id)
        info   @(work/pull! [{:user/city [:city/geometry :city/bbox :city/name]}
                             {:user/route [:route/routes]}
                             :user/goal]
                            [:user/id id])
        route   (first (:route/routes (:user/route info)))
        path    (map geometry/latlng (:coordinates (:geometry route)))
        poi     (:text (:user/goal info))]
    [:> react/ScrollView {:flex 1}
      [:> react/View {:height (* 0.9 (:height window))}
        [symbols/CityMap info
          [:> expo/MapPolyline {:coordinates path
                                :strokeColor "#3bb2d0"
                                :strokeWidth 4}]]]
      [:> react/View {:height (* 1.1 (:height window)) :flexDirection "row"
                      :backgroundColor "white"}
        [:> react/View {:flex 4 :height "7%"}
          [Transfers window]
          [:> react/Text {:style {:color "gray"}} poi]]
        [:> react/Text {:style {:flex 5 :color "gray" :paddingTop "2.5%"
                                :paddingLeft "10%"}}
                       (duration/format (* 1000 (:duration route)))]]
      [:> react/View (merge (symbols/circle 52) symbols/shadow
                            {:position "absolute" :right "10%"
                             :top (* 0.88 (:height window))})
        [:> expo/Ionicons {:name "ios-navigate" :size 62 :color "blue"}]]]))

(def Screen    (rn-nav/stack-screen Instructions
                                    {:title "directions"}))

;hive.rework.state/conn
;(into {} (work/entity [:route/uuid #uuid"5b2d247b-f8c6-47f3-940e-dee71f97d451"]))
;(work/q queries/routes-ids)
