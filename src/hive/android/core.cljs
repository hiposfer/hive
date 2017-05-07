(ns hive.android.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.router :as router]
            [re-frame.subs :as subs]
            [re-frame.fx :as fx]
            [hive.events :as events :refer [before]]
            [hive.subs :as query]
            [hive.effects :as effects]
            [hive.secrets :as secrets]
            [hive.foreigns :as fl]
            [hive.android.screens :as screens]))

(js* "/* @flow */") ;; TODO

;; I use the following convention for effects, subscriptions and event handlers
; - the app-state (:db) is sacred, so only store resources values there
; - whenever keeping track of a sequence of transformations, use interceptors for that
; - all registrations should be done on init. That way you have pure functions and clean requires

(defn app-root []
  (let [screen (subs/subscribe [:view/screen])]
    (condp = @screen
      :home [screens/home]
      :setting [screens/settings])))

(defn init []
  ;;------------- effect handlers --------------
  ; effects is a function of [values] -> void
  (fx/register :fetch/json (fn fetch-json [[url options handler]]
                             (effects/fetch url options effects/res->json handler)))
  (fx/register :app/exit   (fn [v] (.exitApp fl/back-android)))
  (fx/register :map/fly-to (fn [[map-ref lat lng zoom]] (.setCenterCoordinateZoomLevel map-ref lat lng zoom)))
  ;; TODO: avoid having such long paremeters, prefer a simple default to simplify the function
  (fx/register :map/bound  (fn [[map-ref latSW lngSW latNE lngNE padTop padRight padDown padLeft]]
                             (when map-ref
                               (.setVisibleCoordinateBounds map-ref latSW lngSW latNE lngNE padTop padRight padDown padLeft))))
  ;; ------------- event handlers -------------
  ;`db-handler` is a function: (db event) -> db
  (rf/reg-event-db :hive/state events/init) ;;FIXME validate-spec
  (rf/reg-event-db :map/ref events/assoc-rf)
  (rf/reg-event-db :user/city (fn [db [id v]] (assoc db id v :map/camera [(:center v)])))
  (rf/reg-event-db :user/location (fn [db [id gps]] (assoc db id (js->clj gps :keywordize-keys true))))
  (rf/reg-event-db :view.home/targets events/assoc-rf)
  (rf/reg-event-db :view/screen events/assoc-rf)
  (rf/reg-event-db :view/side-menu events/assoc-rf)
  ;; fx-handlers is a function [coeffects event] -> effects
  (rf/reg-event-fx :user/goal events/destination)
  (rf/reg-event-fx :map/annotations [(before events/carmen->targets)] events/targets)
  (rf/reg-event-fx :map/geocode [(before events/bypass-geocode) (before events/bias-geocode)]
                                events/geocode)
  (rf/reg-event-fx :map/directions [(before events/bypass-directions)] events/directions)
  (rf/reg-event-fx :map/camera events/move-camera);; effect proxy to allow calling dispatch on it
  (rf/reg-event-fx :view/return events/navigate-back)
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :view.home/targets query/get-rf)
  (subs/reg-sub :view/side-menu query/get-rf)
  (subs/reg-sub :view/screen query/get-rf)
  (subs/reg-sub :map/annotations query/get-rf)
  (subs/reg-sub :user/location query/get-rf)
  (subs/reg-sub :user/city query/get-rf)
  ;; App init
  (.setAccessToken fl/MapBox (:mapbox secrets/tokens))
  (.initializeApp fl/FireBase (clj->js (:firebase secrets/tokens)))
  (fl/on-back-button (fn [] (do (router/dispatch [:view/return true]) true)))
  (router/dispatch-sync [:hive/state]);(dispatch-sync [:initialize-db])
  (.registerComponent fl/app-registry "Hive" #(r/reactify-component app-root)))

;; It works !!!
;(.set (.ref (.database fl/FireBase) "hello") (clj->js {:name "na du"}))