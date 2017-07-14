(ns hive.wrappers.mapbox
  "See https://github.com/mapbox/react-native-mapbox-gl/blob/master/API.md"
  (:require [hive.geojson :as geojson]
            [hive.foreigns :as fl]
            [clojure.string :as str]
            [hive.util :as util]))

(defn init! [token] (.setAccessToken fl/MapBox token))

(defn box-map!
  "modify the mapview to display the area specified in the parameters"
  [[map-ref geojson]]
  (let [[min-lon, min-lat, max-lon, max-lat] (geojson/bbox geojson)
        padding                              (:padding (:properties (:geometry geojson)))
        [padTop padRight padDown padLeft]    (or padding [50 50 50 50])]
    (when map-ref
      (.setVisibleCoordinateBounds map-ref ; latSW lngSW latNE lngNE
                                   min-lat min-lon max-lat max-lon
                                   padTop padRight padDown padLeft))))

(defn center&zoom!
  "takes a mapview reference and a feature point geojson and moves the map
  to the point coordinates with respective zoom level"
  [[map-ref feat-point]]
  (let [zoom      (:zoom (:properties feat-point))
        [lon lat] (:coordinates (:geometry feat-point))]
    (.setCenterCoordinateZoomLevel map-ref lat lon (:zoom zoom))))

(defn- complete-geocode-event
  "takes the parameters passed to create a MapBox geocode call and inserts the api
   token into the events parameters to avoid overly long events calls"
  [db [id query handler]]
  (let [token     (:mapbox (:tokens db))
        params    {:access_token token}
        position  (:user/location db)
        prox      (some->> position geojson/uri)
        bounds    (str/join "," (geojson/bbox (:user/city db)))]
    (if (nil? position)
      [id "mapbox.places" query (merge params {:bbox bounds}) handler]
      [id "mapbox.places" query (merge params {:proximity prox :bbox bounds}) handler])))

(defn get-places
  "call mapbox geocode api v5 with the provided parameters.
  See https://www.mapbox.com/api-documentation/#request-format"
  [cofx event]
  (let [[_ kind query url-param handler] (complete-geocode-event (:db cofx) event)
        template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}"
        params   (reduce-kv (fn [res k v] (conj res (str (name k) "=" (js/encodeURIComponent v))))
                            [] url-param)
        URL (-> (str/replace template "{mode}" kind)
                (str/replace "{query}" (js/encodeURIComponent query))
                (str/replace "{params}" (str/join "&" params)))]
    {:fetch/json [URL {} handler]}))

(defn- complete-directions-event
  "takes the parameters passed to create a MapBox directions call and inserts the api
   token into the events parameters to avoid overly long events calls"
  [db [id coordinates handler]]
  (let [token (:mapbox (:tokens db))]
    [id "mapbox/driving" coordinates
     {:access_token token :geometries "geojson"} ; :steps true}
     handler]))

(defn get-directions
  "call mapbox directions api v5 with the provided parameters.
  See https://www.mapbox.com/api-documentation/#directions"
  [cofx event]
  (let [[_ profile dst url-param handler] (complete-directions-event (:db cofx) event)
        template "https://api.mapbox.com/directions/v5/{profile}/{coordinates}.json?{params}"
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

(defn move-camera
  "takes a point coordinate (lat, lon) and an (optional) zoom and makes the
   mapview fly to it"
  [cofx [_ feature]]
  (let [[lon lat] (:coordinates (:geometry feature))
        zoom      (:zoom (:properties feature))
        map-ref   (:map/ref (:db cofx))]
    (if (nil? map-ref) {}
      {:map/fly-to [map-ref lat lon (or zoom hive.core/default-zoom)]})))

(defn show-places
  "Handles the events of the result of a geocode search, i.e. possible routing
  targets"
  [cofx [_ carmen-geojson]]
  (let [native (js->clj carmen-geojson :keywordize-keys true)
        annotations (into [] (comp (map #(assoc % :title (first (str/split (:place_name %) #","))))
                                   (map #(assoc % :subtitle (:address (:properties %)))))
                          (:features native))
        base   {:db (assoc (:db cofx) :map/annotations annotations
                                      :view.home/targets (pos? (count annotations)))}]
    (if (empty? annotations) base
      (assoc base :map/bound [(:map/ref (:db cofx)) native]))))

;; https://www.mapbox.com/api-documentation/#directions-response-object
(defn show-directions
  "takes the result of mapbox directions api and causes both db and mapview
  to update accordingly"
  [cofx [_ directions]]
  (let [dirs    (js->clj directions :keywordize-keys true)
        linestr (:geometry (first (:routes dirs)))
        goal    (geojson/feature "Point" (last (:coordinates linestr)) {:title "goal"
                                                                        :id (str (last (:coordinates linestr)))})
        route   (util/polyline {:type "Feature" :geometry linestr})
        base    {:db (assoc (:db cofx) :map/annotations [goal route];; remove all targets
                                       :view.home/targets false)
                 :map/bound [(:map/ref (:db cofx)) linestr]}]
    base))