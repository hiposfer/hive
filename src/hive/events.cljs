(ns hive.events
  (:require [clojure.spec :as s]
            [hive.core :as hive]
            [re-frame.std-interceptors :as nsa]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [hive.secrets :as secrets]))

;; -- Interceptors ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check after " event " failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (nsa/after (partial check-and-throw ::hive/state))
    []))

(def map-token
  (rf/->interceptor     ;; a utility function supplied by re-frame
    :id :map.default/geocode  ;; ids are decorative only
    :before (fn [context] ;; extract db and event from coeffects
              (let [[id name handler] (:event (:coeffects context))]
                (assoc-in context [:coeffects :event]
                  [id "mapbox.places" name {:access_token (:mapbox secrets/tokens)} handler])))))

;; -- Handlers --------------------------------------------------------------

(defn init [_ _] hive/state)

(defn geocode
  [cofx [id kind query url-param handler]]
  (let [template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}"
        params   (reduce-kv (fn [res k v] (conj res (str (name k) "=" (js/encodeURIComponent v))))
                            [] url-param)
        URL (-> (str/replace template "{mode}" kind)
                (str/replace "{query}" (js/encodeURIComponent query))
                (str/replace "{params}" (str/join "&" params)))]
    {:fetch/json [URL {} handler]}))

;(r/reg-event-db :set-greeting
;  validate-spec
;  (fn [db [_ value]] (assoc db :greeting value)))
