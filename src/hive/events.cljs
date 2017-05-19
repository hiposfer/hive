(ns hive.events
  (:require [clojure.spec :as s]
            [hive.core :as hive]
            [re-frame.std-interceptors :as nsa]
            [re-frame.interceptor :as fbi]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [hive.secrets :as secrets]
            [hive.effects :as effects]
            [re-frame.router :as router]
            [hive.foreigns :as fl]
            [hive.util :as util]
            [hive.geojson :as geojson]))

;; -- Handlers --------------------------------------------------------------

(defn init [_ _] hive/state)

(defn geocode
  "call mapbox geocode api v5 with the provided parameters.
  See https://www.mapbox.com/api-documentation/#request-format"
  [cofx [id kind query url-param handler]]
  (let [template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}"
        params   (reduce-kv (fn [res k v] (conj res (str (name k) "=" (js/encodeURIComponent v))))
                            [] url-param)
        URL (-> (str/replace template "{mode}" kind)
                (str/replace "{query}" (js/encodeURIComponent query))
                (str/replace "{params}" (str/join "&" params)))]
    {:fetch/json [URL {} handler]}))

(defn directions
  "call mapbox directions api v5 with the provided parameters.
  See https://www.mapbox.com/api-documentation/#directions"
  [cofx [id profile dst url-param handler]]
  (let [template "https://api.mapbox.com/directions/v5/{profile}/{coordinates}.json?{params}"
        params  (reduce-kv (fn [res k v] (conj res (str (name k) "=" (js/encodeURIComponent v))))
                           [] url-param)
        gps     (:user/location (:db cofx))
        query   (str (geojson/uri gps) ";" (geojson/uri dst))
        URL     (-> (str/replace template "{profile}" profile)
                    (str/replace "{coordinates}" (js/encodeURIComponent query))
                    (str/replace "{params}" (str/join "&" params)))]
    (if (nil? gps)
      (do (println "MISSING GPS POSITION") {});;TODO: handle this properly
      {:fetch/json [URL {} handler]})))

(defn navigate-back
  "modifies the behavior of the back button on Android according to the view
  currently active. It defaults to Exit app if no screen was found"
  [cofx [id]]
  (condp = (:view/screen (:db cofx))
    :home (if (:view.home/targets (:db cofx))
            {:db (assoc (:db cofx) :view.home/targets false)}
            {:app/exit true})
    :setting {:db (assoc (:db cofx) :view/screen :home)}
    {:app/exit true}))

(defn assoc-rf
  "basic event handler. It assocs the event id with its value"
  [db [id v]] (assoc db id v))

(defn move-camera
  "takes a point coordinate (lat, lon) and an (optional) zoom and makes the
   mapview fly to it"
  [cofx [id feature]]
  (let [[lon lat] (:coordinates (:geometry feature))
        zoom      (:zoom (:properties feature))
        map-ref   (:map/ref (:db cofx))]
    (if (nil? map-ref) {}
      {:map/fly-to [map-ref lat lon (or zoom hive.core/default-zoom)]})))

(defn targets
  "Handles the events of the result of a geocode search, i.e. possible routing
  targets"
  [cofx [id carmen-geojson]]
  (let [native (js->clj carmen-geojson :keywordize-keys true)
        titles (map #(hash-map :title (first (str/split (:place_name %) #","))) (:features native))
        subs   (map #(hash-map :subtitle (:address (:properties %))) (:features native))
        flat   (map util/feature->verbose (:features native))
        annotations (map merge titles subs (map #(update % :coordinates reverse) flat))
        base   {:db (assoc (:db cofx) :map/annotations (map #(update % :type str/lower-case) annotations)
                                      ;:user/targets native
                                      :view.home/targets (pos? (count annotations)))}]
    ;(cljs.pprint/pprint annotations)
    (if (empty? annotations) base
      (assoc base :map/bound [(:map/ref (:db cofx)) native]))))

(defn destination
  "takes the result of mapbox directions api and causes both db and mapview
  to update accordingly"
  [cofx [id directions]]
  (let [dirs    (js->clj directions :keywordize-keys true)
        linestr (:geometry (first (:routes dirs)))
        ;start   (effects/mark (reverse (first linestr)) "start")
        goal    (util/marker (reverse (last (:coordinates linestr))) "goal")
        route   (util/polyline (map reverse (:coordinates linestr)))
        base    {:db (assoc (:db cofx) :map/annotations [goal route];; remove all targets
                                       :view.home/targets false)
                 :map/bound [(:map/ref (:db cofx)) linestr]}]
    base))


;;(cljs.pprint/pprint base)