(ns hive.android.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.router :as router]
            [re-frame.subs :as subs]
            [re-frame.fx :as fx]
            [re-frame.std-interceptors :as nsa]
            [hive.events :as events]
            [hive.subs :as query]
            [hive.effects :as effects]
            [hive.core :as hive]
            [hive.secrets :as secrets]
            [clojure.string :as str]))

(defn show-places
  [carmen-geojson]
  (let [native     (js->clj carmen-geojson :keywordize-keys true)
        interest   (->> (filter (comp #{"Point"} :type :geometry) (:features native))
                        (sort-by :relevance)
                        (take 5))
        points     (map (comp reverse :coordinates :geometry) interest)
        names      (map (comp first #(str/split % #",") :place_name) interest)
        markers    (sequence (map effects/mark) points names)]
    (cljs.pprint/pprint interest)
    (router/dispatch [:map/annotations (clj->js markers)])))


;(router/dispatch [:map/geocode
;                  "Göethe Universität, frankfurt"
;                  show-places])

(def ReactNative (js/require "react-native"))
(def MapBox (js/require "react-native-mapbox-gl"))
(def logo-img (js/require "./images/cljs.png"))

(def app-registry (.-AppRegistry ReactNative))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def button (r/adapt-react-class (.-Button ReactNative)))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
;(def image (r/adapt-react-class (.-Image ReactNative)))
;(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def mapview (r/adapt-react-class (.-MapView MapBox)))
(defn alert [title] (.alert (.-Alert ReactNative) title))

(defn app-root []
  (let [map-center (subs/subscribe [:map/center])
        map-zoom   (subs/subscribe [:map/zoom])
        map-annotations (subs/subscribe [:map/annotations])]
    [view {:style {:flex 1}}
     [mapview {:style {:flex 9} :initialZoomLevel @map-zoom :annotationsAreImmutable true
               :initialCenterCoordinate @map-center :annotations @map-annotations
               :showsUserLocation true}]
     [view {:style {:flex 1 :flexDirection "row" :background-color "teal" :align-items "center"}}
      [text-input {:style {:flex 18 } :placeholderTextColor "white" :placeholder "where would you like to go?"
                   :onChangeText #(router/dispatch [:map/geocode % show-places])}]
      [button {:style {:flex 3} :accessibilityLabel "search best route"
               :title "GO" :color "#841584" :on-press #(alert (str "Hello " %1))}]]]))
       ;;[image {:source logo-img
       ;;        :style  {:width 80 :height 80 :margin-bottom 30}]
       ;;[touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
       ;;                      :on-press #(alert "HELLO CHUBBY!")}}
       ;; [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Alert"]]])))

(defn init []
  ;;------------- effect handlers --------------
  ;(fx/register :fx/debounce effects/debounce)
  (fx/register :fetch/json (fn fetch-json [[url options handler]]
                             (effects/fetch url options effects/res->json handler)))
  ;; ------------- event handlers -------------
  (rf/reg-event-db :hive/state events/init) ;;FIXME validate-spec
  (rf/reg-event-fx :map/geocode [events/map-token] events/geocode)
  (rf/reg-event-db :map/annotations (fn [db [id annotations]] (assoc db id annotations)))
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :map/center query/map-center)
  (subs/reg-sub :map/zoom query/map-zoom)
  (subs/reg-sub :map/annotations query/map-annotations)
  ;; App init
  (.setAccessToken MapBox (:mapbox secrets/tokens))
  (router/dispatch-sync [:hive/state]);(dispatch-sync [:initialize-db])
  (.registerComponent app-registry "Hive" #(r/reactify-component app-root)))

;(router/dispatch [:map/annotations [{:coordinates [50.0876, 8.6451]
;                                     :type "point"
;                                     :tytle "casa"
;                                     :id (str (hash [50.0876, 8.6451]))}]])
