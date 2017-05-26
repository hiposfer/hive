(ns hive.android.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.router :as router]
            [re-frame.subs :as subs]
            [re-frame.fx :as fx]
            [hive.events :as events]
            [hive.subs :as query]
            [hive.effects :as effects]
            [hive.secrets :as secrets]
            [hive.foreigns :as fl]
            [hive.android.screens :as screens]
            [hive.interceptors :as hijack :refer [before]]
            [hive.wrappers.firebase :as firebase]
            [hive.wrappers.mapbox :as mapbox]))

(js* "// @flow")

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
  (fx/register :fetch/json effects/retrieve->json!)
  (fx/register :app/exit effects/quit!)
  (fx/register :firebase.auth/anonymous firebase/sign-in-anonymously!)
  (fx/register :map/fly-to mapbox/center&zoom!)
  (fx/register :map/bound mapbox/box-map!)
  ;; ------------- event handlers -------------
  ;`db-handler` is a function: (db event) -> db
  (rf/reg-event-db :hive/state hijack/validate events/init)
  (rf/reg-event-db :map/ref    hijack/validate events/assoc-rf)
  (rf/reg-event-db :user/city  hijack/validate events/move-out)
  (rf/reg-event-db :user/location     hijack/validate events/assoc-rf)
  (rf/reg-event-db :view.home/targets hijack/validate events/assoc-rf)
  (rf/reg-event-db :view/screen    hijack/validate events/assoc-rf)
  (rf/reg-event-db :view/side-menu hijack/validate events/assoc-rf)
  ;; fx-handlers is a function [coeffects event] -> effects
  (rf/reg-event-fx :user/goal       events/destination);; json object not geojson conformen
  (rf/reg-event-fx :map/annotations events/targets)
  (rf/reg-event-fx :map/geocode [(before hijack/bypass-geocode) (before hijack/bias-geocode)]
                                events/geocode)
  (rf/reg-event-fx :map/directions (before hijack/bypass-directions) events/directions)
  (rf/reg-event-fx :map/camera  events/move-camera);; effect proxy to allow calling dispatch on it
  (rf/reg-event-fx :view/return hijack/validate events/navigate-back)
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :view.home/targets query/get-rf)
  (subs/reg-sub :view/side-menu    query/get-rf)
  (subs/reg-sub :view/screen       query/get-rf)
  (subs/reg-sub :map/annotations   query/get-rf)
  (subs/reg-sub :user/location     query/get-rf)
  (subs/reg-sub :user/city         query/get-rf)
  ;; App init
  (mapbox/init! (:mapbox secrets/tokens))
  (firebase/init! (clj->js (:firebase secrets/tokens)))
  (fl/on-back-button (fn [] (do (router/dispatch [:view/return true]) true)))
  (router/dispatch-sync [:hive/state]);(dispatch-sync [:initialize-db])
  (.registerComponent fl/app-registry "Hive" #(r/reactify-component app-root)))

;; It works !!!
;(.set (.ref (.database fl/FireBase) "hello") (clj->js {:name "na du"}))

;; TODO: restore the latest targets whenever the text input get focus again
;; remove text input focus on any other component press