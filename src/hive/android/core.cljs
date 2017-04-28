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
; - whenever keeping track of a sequence of transformations, use interceptors for that
; - all registrations should be done on init. That way you have pure functions and clean requires

(def menu-img (js/require "./images/ic_menu.png"))

(defn app-root []
  (let [map-camera    (subs/subscribe [:map/camera])
        map-markers   (subs/subscribe [:user/targets])
        view-targets? (subs/subscribe [:view.home/targets])
        menu-open?    (subs/subscribe [:view/side-menu])]
    [c/side-menu {:style {:flex 1} :menu (r/as-element c/menu) :isOpen @menu-open?
                  :onChange (fn [s] (when-not (= @menu-open? s) (router/dispatch [:view/side-menu s])))}
      [c/view {:style {:height 50 :flexDirection "row" :background-color "teal"
                       :align-items "center"}}
        [c/touchable-highlight {:on-press #(router/dispatch [:view/side-menu (not @menu-open?)])}
          [c/image {:source menu-img}]]
        ;; TODO: throttle geocoding
        [c/text-input {:style {:flex 9} :placeholderTextColor "white" :placeholder "where would you like to go?"
                       :onChangeText (fn [v] (router/dispatch [:map/geocode v #(router/dispatch [:user/targets %])]))}]]
        ;[c/button {:style {:flex 1} :accessibilityLabel "search best route"
        ;           :title "GO" :color "#841584" :on-press #(fl/alert (str "Hello " %1))}]]
      (when @view-targets?
        [c/targets-list @map-markers])
      [c/mapview {:style {:flex 3} :initialZoomLevel (:zoom @map-camera) :annotationsAreImmutable true
                  :initialCenterCoordinate (:center @map-camera) :annotations (clj->js @map-markers)
                  :showsUserLocation true ;:ref (fn [this] (println "this: " this)) ;(when this (.keys this))))
                  :onUpdateUserLocation #(router/dispatch [:user/location %])
                  :onTap #(router/dispatch [:view.home/targets false])
                  :ref (fn [mv] (router/dispatch [:map/ref mv]))}]]))

(defn init []
  ;;------------- effect handlers --------------
  ; effects is a function of [values] -> void
  (fx/register :fetch/json (fn fetch-json [[url options handler]]
                             (effects/fetch url options effects/res->json handler)))
  (fx/register :app/exit   (fn [v] (.exitApp fl/back-android)))
  (fx/register :map/fly-to (fn [[map-ref lat lng zoom]] (.setCenterCoordinateZoomLevel map-ref lat lng zoom)))
  (fx/register :map/bound  (fn [[map-ref latSW lngSW latNE lngNE padTop padRight padDown padLeft]]
                             (when map-ref
                               (.setVisibleCoordinateBounds map-ref latSW lngSW latNE lngNE padTop padRight padDown padLeft))))
  ;; ------------- event handlers -------------
  ;`db-handler` is a function: (db event) -> db
  (rf/reg-event-db :hive/state events/init) ;;FIXME validate-spec
  (rf/reg-event-db :map/ref events/assoc-rf)
  (rf/reg-event-db :user/location (fn [db [id gps]] (assoc db id (js->clj gps :keywordize-keys true))))
  (rf/reg-event-db :view.home/targets events/assoc-rf)
  (rf/reg-event-db :view/screen events/assoc-rf)
  (rf/reg-event-db :view/side-menu events/assoc-rf)
  ;; fx-handlers is a function [coeffects event] -> effects
  (rf/reg-event-fx :user/targets [(before events/carmen->targets)] events/targets)
  (rf/reg-event-fx :map/geocode [(before events/map-token) (before events/bias-geocode)]
                                events/geocode)
  (rf/reg-event-fx :map/camera events/fly-to)
  (rf/reg-event-fx :view/return events/navigate-back)
  ;; ------------- queries ---------------------------------
  (subs/reg-sub :map/ref query/get-rf)
  (subs/reg-sub :map/camera query/get-rf)
  (subs/reg-sub :view.home/targets query/get-rf)
  (subs/reg-sub :view/side-menu query/get-rf)
  (subs/reg-sub :view/screen query/get-rf)
  (subs/reg-sub :user/targets query/get-rf)
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