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

(js* "/* @flow */") ;; TODO

;; I use the following convention for effects, subscriptions and event handlers
; - the app-state (:db) is sacred, so only store resources values there
; - whenever keeping track of a sequence of transformations, use coeffects for that
; - all registrations should be done on init. That way you have pure functions and clean requires

;(router/dispatch [:map/geocode
;                  "Göethe Universität, frankfurt"
;                  show-places])

;(def logo-img (js/require "./images/cljs.png"))

(defn app-root []
  (let [city       (subs/subscribe [:user/city])
        map-markers (subs/subscribe [:user/targets])
        view-targets? (subs/subscribe [:view/targets])]
    [c/view {:style {:flex 1}}
      [c/view {:style {:height 50 :flexDirection "row" :background-color "teal"
                       :align-items "center"}}
        ;; TODO: throttle
        [c/text-input {:style {:flex 9} :placeholderTextColor "white" :placeholder "where would you like to go?"
                       :onChangeText (fn [v] (router/dispatch [:map/geocode v #(router/dispatch [:user/targets %])]))}]
        [c/button {:style {:flex 1} :accessibilityLabel "search best route"
                   :title "GO" :color "#841584" :on-press #(fl/alert (str "Hello " %1))}]]
      (when @view-targets?
        [c/targets-list @map-markers])
      [c/mapview {:style {:flex 3} :initialZoomLevel (:zoom @city) :annotationsAreImmutable true
                  :initialCenterCoordinate (:center @city) :annotations (clj->js @map-markers)
                  ;FIXME: get a ref to dynamically change the bounds
                  :showsUserLocation true ;:ref (fn [this] (println "this: " this)) ;(when this (.keys this))))
                  :onUpdateUserLocation #(router/dispatch [:user/location %])
                  :onTap #(router/dispatch [:view/targets false])}]]))

(defn init []
  ;;------------- effect handlers --------------
  (fx/register :fetch/json (fn fetch-json [[url options handler]]
                             (effects/fetch url options effects/res->json handler)))
  (fx/register :app/exit (fn [v] (.exitApp fl/back-android)))
  ;; ------------- event handlers -------------
  (rf/reg-event-db :hive/state events/init) ;;FIXME validate-spec
  (rf/reg-event-fx :map/geocode [(before events/map-token) (before events/bias-geocode)]
                   events/geocode)
  (rf/reg-event-db :user/targets [(before events/carmen->targets)]
                   (fn [db [id annotations]]
                     (merge db {:user/targets (reverse annotations)
                                :view/targets (pos? (count annotations))})))
  (rf/reg-event-db :user/location (fn [db [id gps]] (assoc db id (js->clj gps :keywordize-keys true))))
  (rf/reg-event-db :view/targets (fn [db [id v]] (assoc db id v)))
  (rf/reg-event-db :view/screen (fn [db [id v]] (assoc db id v))) ;;FIXME validate-spec
  (rf/reg-event-fx :view/return events/navigate-back)
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :view/targets query/view-targets?)
  (subs/reg-sub :view/screen query/view-screen)
  (subs/reg-sub :user/targets query/user-targets)
  (subs/reg-sub :user/location query/user-location)
  (subs/reg-sub :user/city query/user-city)
  ;; App init
  (.setAccessToken fl/MapBox (:mapbox secrets/tokens))
  (.initializeApp fl/FireBase (clj->js (:firebase secrets/tokens)))
  (fl/on-back-button (fn [] (do (router/dispatch [:view/return true]) true)))
  (router/dispatch-sync [:hive/state]);(dispatch-sync [:initialize-db])
  (.registerComponent fl/app-registry "Hive" #(r/reactify-component app-root)))

;; IT works !!!
;(.set (.ref (.database fl/FireBase) "hello") (clj->js {:name "na du"}))