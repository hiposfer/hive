(ns hive.components.screens.home.route
  (:require [hive.queries :as queries]
            [hive.components.foreigns.react :as react]
            [hive.rework.util :as tool]
            [datascript.core :as data]
            [hive.rework.core :as work]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.components.symbols :as symbols]
            [hive.foreigns :as fl]
            [hive.components.foreigns.expo :as expo]
            [hive.libs.geometry :as geometry]
            [goog.date.duration :as duration]))

;(.. DateTime (fromRfc822String "2018-05-07T10:15:30"))

(defn process-directions
  "takes a mapbox directions response and returns it.
   Return an exception if no path was found"
  [path user now]
  (if (not= (goog.object/get path "code"))
    (ex-info (goog.object/get "msg") path)
    (let [path (js->clj path :keywordize-keys true)]
      [(tool/with-ns :route (assoc path :departure now))
       {:user/id user
        :user/route [:route/uuid (:uuid path)]}])))

(def big-circle (symbols/circle 16))
(def small-circle (symbols/circle 12))

(defn- SectionDetails
  [data]
  [:> react/View {:style {:flex 9}}
    [:> react/Text (some (comp not-empty :name) data)]])

(defn- TransitSection
  [data]
  [:> react/View {:flex 2 :flexDirection "row"}
    [:> react/View {:flex 1 :alignItems "center"}
      [:> react/Text {:style {:color "gray" :fontSize 12}}
                     "21:54"]]
    [:> react/View {:flex 1 :alignItems "center"}
      [:> react/View (merge {:backgroundColor "red"} small-circle)]
      [:> react/View {:backgroundColor "red" :width "8%" :height "93%"}]
      [:> react/View (merge {:backgroundColor "red" :elevation 10}
                            small-circle)]]])

(defn- WalkingSection
  [data transfer?]
  [:> react/View {:flex 2 :flexDirection "row"}
    [:> react/View {:flex 1 :alignItems "center"}
      [:> react/Text {:style {:color "gray" :fontSize 12}}
                     "21:54"]]
    [:> react/View {:flex 1 :alignItems "center"
                    :justifyContent "space-around"}
      (for [i (range 5)]
        (if (and transfer? (= 0 i))
          ^{:key i} [:> react/View (merge {:backgroundColor "transparent"}
                                          small-circle)]
          ^{:key i} [:> react/View (merge {:backgroundColor "gray"}
                                          small-circle)]))]])

(defn- Route
  [props user]
  (let [data   @(work/pull! [{:user/route [:route/route :route/uuid]}]
                            [:user/id user])
        route     (:route/route (:user/route data))
        sections  (partition-by :mode (:steps route))]
    [:> react/View props
      (for [part sections]
        ^{:key (:distance (first part))}
         [:> react/View {:flex 9 :flexDirection "row"}
           (if (= "walking" (some :mode part))
             [WalkingSection part (not= part (first sections))]
             [TransitSection part])
           [SectionDetails part]])]))

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
  [props user]
  (let [data   @(work/pull! [{:user/route [:route/route]}
                             :user/goal]
                            [:user/id user])
        route   (:route/route (:user/route data))
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
        id      (data/q queries/user-id (work/db))
        data   @(work/pull! [{:user/route [:route/route :route/uuid]}]
                            [:user/id id])
        path    (sequence (comp (map :geometry)
                                (map :coordinates)
                                (mapcat #(drop-last %))
                                (map geometry/latlng))
                          (:steps (:route/route (:user/route data))))]
    [:> react/ScrollView {:flex 1}
      [:> react/View {:height (* 0.9 (:height window))}
        [symbols/CityMap id
          [:> expo/MapPolyline {:coordinates path
                                :strokeColor "#3bb2d0"
                                :strokeWidth 4}]]]
      [:> react/View {:height (* 1.1 (:height window)) :backgroundColor "white"}
        [Info {:flex 1 :paddingTop "1%"} id]
        [Route {:flex 9} id]]
      [:> react/View (merge (symbols/circle 52) symbols/shadow
                            {:position "absolute" :right "10%"
                             :top (* 0.88 (:height window))})
        [:> expo/Ionicons {:name "ios-navigate" :size 62 :color "blue"}]]]))

(def Screen    (rn-nav/stack-screen Instructions
                                    {:title "directions"}))

;hive.rework.state/conn
;(into {} (work/entity [:route/uuid #uuid"5b2d247b-f8c6-47f3-940e-dee71f97d451"]))
;(work/q queries/routes-ids)

;(let [id      (data/q queries/user-id (work/db))
;      data   @(work/pull! [{:user/route [:route/route :route/uuid]}]
;                          [:user/id id])]
;  ;(sequence (comp (map :geometry)
;  ;                (mapcat :coordinates)
;  ;                (map geometry/latlng))
;  (:steps (:route/route (:user/route data))))
