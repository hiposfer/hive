(ns hive.android.core
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.router :as router]
            [re-frame.subs :as subs]
            [re-frame.fx :as fx]
            [hive.events :as events :refer [before]]
            [hive.subs :as query]
            [hive.effects :as effects]
            [hive.secrets :as secrets]
            [hive.foreigns :as fl]
            [hive.components :as c]))

;; I use the following convention for effects, subscriptions and event handlers
; - the app-state (:db) is sacred, so only store resources values there
; - whenever keeping track of a sequence of transformations, use coeffects for that
; - all registrations should be done on init. That way you have pure functions and clean requires

;(router/dispatch [:map/geocode
;                  "Göethe Universität, frankfurt"
;                  show-places])

;(def logo-img (js/require "./images/cljs.png"))

(defn app-root []
  (let [map-center (subs/subscribe [:map/center])
        map-zoom   (subs/subscribe [:map/zoom])
        map-markers (subs/subscribe [:user/targets])
        view-targets? (subs/subscribe [:view/targets])]
    [c/view {:style {:flex 1}}
     [c/mapview {:style {:flex 3} :initialZoomLevel @map-zoom :annotationsAreImmutable true
                 :initialCenterCoordinate @map-center :annotations (clj->js @map-markers)
                 :showsUserLocation true ;:userTrackingMode (-> MapBox .-userTrackingMode .-followWithCourse)
                 :onUpdateUserLocation #(router/dispatch [:user/location %])
                 :onOpenAnnotation #(println "annotation: " %)
                 :onLongPress #(println "long-press: " %)
                 :onTap #(when @view-targets? (router/dispatch [:view/targets false]))}]
     (when @view-targets?
       [c/targets-list @map-markers])
     [c/view {:style {:height 50 :flexDirection "row" :background-color "teal"
                      :align-items "center"}}
      ;; TODO: throttle
      [c/text-input {:style {:flex 9} :placeholderTextColor "white" :placeholder "where would you like to go?"
                     :onChangeText (fn [v] (router/dispatch [:map/geocode v #(router/dispatch [:user/targets %])]))}]
      [c/button {:style {:flex 1} :accessibilityLabel "search best route"
                 :title "GO" :color "#841584" :on-press #(fl/alert (str "Hello " %1))}]]]))
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
  (rf/reg-event-fx :map/geocode [(before events/map-token) (before events/bias-geocode)]
                   events/geocode)
  (rf/reg-event-db :user/targets [(before events/carmen->targets)]
                   (fn [db [id annotations]] (merge db {:user/targets (reverse annotations)
                                                        :view/targets (pos? (count annotations))})))
  (rf/reg-event-db :user/location (fn [db [id gps]] (assoc db id (js->clj gps :keywordize-keys true))))
  (rf/reg-event-db :view/targets (fn [db [id v]] (assoc db :view/targets v)))
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :map/center query/map-center)
  (subs/reg-sub :map/zoom query/map-zoom)
  (subs/reg-sub :view/targets query/view-targets?)
  (subs/reg-sub :user/targets query/user-targets)
  (subs/reg-sub :user/location query/user-location)
  ;; App init
  (.setAccessToken fl/MapBox (:mapbox secrets/tokens))
  (.initializeApp fl/FireBase (clj->js (:firebase secrets/tokens)))
  (router/dispatch-sync [:hive/state]);(dispatch-sync [:initialize-db])
  (.registerComponent fl/app-registry "Hive" #(r/reactify-component app-root)))

;; IT works !!!
;(.set (.ref (.database fl/FireBase) "hello") (clj->js {:name "chubby"}))
