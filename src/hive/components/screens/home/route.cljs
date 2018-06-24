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

(def big-circle (symbols/circle 16))
(def small-circle (symbols/circle 12))

(defn- SectionDetails
  [data]
  [:> react/View {:style {:flex 9}}
    [:> react/Text (some (comp not-empty :name) data)]])

(defn- SectionLine
  [data]
  [:> react/View {:flex 1 :alignItems "center"}
    [:> react/View (merge {:backgroundColor "red"} big-circle)]
    [:> react/View {:backgroundColor "red" :width "8%" :height "70%"}]
    [:> react/View (merge {:backgroundColor "red"} big-circle)]])

(defn- SectionDots
  [data]
  [:> react/View {:flex 1 :alignItems "center" :justifyContent "space-around"}
   [:> react/View (merge {:backgroundColor "gray"} big-circle)]
   (for [i (range 5)]
     ^{:key i}
     [:> react/View (merge {:backgroundColor "gray"} small-circle)])])

(defn- SectionTimes
  [data]
  [:> react/View {:flex 1 :alignItems "center"}
    [:> react/Text {:style {:color "gray" :fontSize 12}}
                   "21:54"]])

(defn- Section
  [data]
  [:> react/View {:flex 9 :flexDirection "row"}
    [SectionTimes data]
    (if (= "walking" (some :mode data))
      [SectionDots data]
      [SectionLine data])
    [SectionDetails data]])

(defn- Route
  [props data]
  (let [steps (:steps (first (:legs (first (:route/routes (:user/route data))))))
        sections (partition-by :mode steps)]
    [:> react/View props
      (for [part sections]
        ^{:key (:distance (first part))}
         [Section part])]))

(defn- Transfers
  []
  (into [:> react/View {:flex 4 :flexDirection "row" :justifyContent "space-around"
                        :top "0.5%"}]
        [[:> expo/Ionicons {:name "ios-walk" :size 32}]
         [:> expo/Ionicons {:name "ios-arrow-forward" :size 26 :color "gray"}]
         [:> expo/Ionicons {:name "ios-bus" :size 32}]
         [:> expo/Ionicons {:name "ios-arrow-forward" :size 26 :color "gray"}]
         [:> expo/Ionicons {:name "ios-train" :size 32}]]))

(defn- Info
  [props data]
  (let [route   (first (:route/routes (:user/route data)))
        poi     (:text (:user/goal data))]
    [:> react/View props
     [:> react/View {:flexDirection "row" :paddingLeft "1.5%"}
      [Transfers]
      [:> react/Text {:style {:flex 5 :color "gray" :paddingTop "2.5%"
                              :paddingLeft "10%"}}
       (duration/format (* 1000 (:duration route)))]]
     [:> react/Text {:style {:color "gray" :paddingLeft "2.5%"}} poi]]))

(defn Instructions
  "basic navigation directions"
  [props]
  (let [window  (tool/keywordize (.. fl/ReactNative (Dimensions/get "window")))
        id      (work/q queries/user-id)
        data   @(work/pull! [{:user/city [:city/geometry :city/bbox :city/name]}
                             {:user/route [:route/routes]}
                             :user/goal]
                            [:user/id id])
        route   (first (:route/routes (:user/route data)))
        path    (map geometry/latlng (:coordinates (:geometry route)))]
    [:> react/ScrollView {:flex 1}
      [:> react/View {:height (* 0.9 (:height window))}
        [symbols/CityMap data
          [:> expo/MapPolyline {:coordinates path
                                :strokeColor "#3bb2d0"
                                :strokeWidth 4}]]]
      [:> react/View {:height (* 1.1 (:height window)) :backgroundColor "white"}
        [Info {:flex 1 :paddingTop "1%"} data]
        [Route {:flex 9} data]]
      [:> react/View (merge (symbols/circle 52) symbols/shadow
                            {:position "absolute" :right "10%"
                             :top (* 0.88 (:height window))})
        [:> expo/Ionicons {:name "ios-navigate" :size 62 :color "blue"}]]]))

(def Screen    (rn-nav/stack-screen Instructions
                                    {:title "directions"}))

hive.rework.state/conn
;(into {} (work/entity [:route/uuid #uuid"5b2d247b-f8c6-47f3-940e-dee71f97d451"]))
;(work/q queries/routes-ids)
