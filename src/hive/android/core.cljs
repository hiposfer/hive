(ns hive.android.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.router :as router]
            [re-frame.subs :as subs]
            [re-frame.fx :as fx]
            [hive.events :as events]
            [hive.subscriptions :refer [get-rf]]
            [hive.effects :as effects]
            [hive.foreigns :as fl]
            [hive.android.screens :as screens]
            [hive.interceptors :as hijack]
            [hive.wrappers.firebase :as firebase]
            [hive.wrappers.mapbox :as mapbox]
            [hive.wrappers.storage :as storage]))

;; I use the following convention for effects, subscriptions and event handlers
; - the app-state (:db) is sacred, so only store resources values there
; - whenever keeping track of a sequence of transformations, use interceptors for that
; - all registrations should be done on init. That way you have pure functions and clean requires

(defn app-root []
  (let [screen (subs/subscribe [:view/screen])]
    (case @screen
      :blockade [screens/blockade]
      :home     [screens/home]
      :setting  [screens/settings]
      :directions [screens/directions])))

(defn init []
  ;;------------- effect handlers --------------
  ; effects is a function of [values] -> void
  (fx/register :fetch/json         effects/retrieve->json!)
  (fx/register :app/exit           effects/quit!)
  (fx/register :app.storage/read   storage/read)
  (fx/register :app.storage/write  storage/write!)
  (fx/register :app.storage/remove storage/remove!)
  (fx/register :app/toast          effects/show-toast!)
  (fx/register :firebase.auth/anonymous firebase/sign-in-anonymously!)
  (fx/register :firebase.db/set         firebase/set-value!)
  (fx/register :firebase/report         firebase/report!)
  (fx/register :map/fly-to         mapbox/center&zoom!)
  (fx/register :map/bound          mapbox/box-map!)
  (fx/register :mapbox/init        mapbox/init!)
  (fx/register :firebase/init      firebase/init!)
  (fx/register :user.input/clear   effects/clear-search-box!)
  ;; ------------- event handlers -------------
  ;`db-handler` is a function: (db event) -> db
  (rf/reg-event-db :map/ref            hijack/validate events/assoc-rf)
  (rf/reg-event-db :user/city          hijack/validate events/update-user-city)
  (rf/reg-event-db :user/location      hijack/validate events/assoc-rf)
  (rf/reg-event-db :view.home/targets  hijack/validate events/assoc-rf)
  (rf/reg-event-db :view/screen        hijack/validate events/assoc-rf)
  (rf/reg-event-db :view/side-menu     hijack/validate events/assoc-rf)
  (rf/reg-event-db :user.goal/route    hijack/validate events/assoc-rf)
  (rf/reg-event-db :user.input/ref     events/assoc-rf)
  ;; fx-handlers is a function [coeffects event] -> effects
  (rf/reg-event-fx :hive/state         hijack/validate effects/init)
  (rf/reg-event-fx :hive/services      events/start-services)
  (rf/reg-event-fx :user/goal          mapbox/show-directions)
  (rf/reg-event-fx :user.input/place   hijack/validate events/on-search-place)
  (rf/reg-event-fx :map/annotations    mapbox/on-geocode-result)
  (rf/reg-event-fx :map.geocode/mapbox mapbox/get-mapbox-places)
  (rf/reg-event-fx :map.geocode/photon mapbox/get-photon-places)
  (rf/reg-event-fx :map/directions     mapbox/get-directions)
  (rf/reg-event-fx :map/camera         mapbox/move-camera);; effect proxy to allow calling dispatch on it
  (rf/reg-event-fx :view/return        hijack/validate events/on-back-button)
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :view.home/targets get-rf)
  (subs/reg-sub :view/side-menu    get-rf)
  (subs/reg-sub :view/screen       get-rf)
  (subs/reg-sub :map/annotations   get-rf)
  (subs/reg-sub :user/location     get-rf)
  (subs/reg-sub :user/city         get-rf)
  (subs/reg-sub :user.input/place  get-rf)
  (subs/reg-sub :user.goal/route   get-rf)
  ;; App init
  (fl/on-back-button (fn [] (do (router/dispatch [:view/return]) true)))
  (router/dispatch-sync [:hive/state]);(dispatch-sync [:initialize-db])
  (.registerComponent fl/app-registry "Hive" #(r/reactify-component app-root)))

;; It works !!!
;(.set (.ref (.database fl/FireBase) "hello") (clj->js {:name "na du"}))

;; TODO: restore the latest targets whenever the text input get focus again
;; remove text input focus on any other component press