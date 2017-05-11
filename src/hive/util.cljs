(ns hive.util
  "functions that come handy every now and then"
  (:require [clojure.set :as set]
            [hive.geojson :as geojson]))

;; MapBox annotations
;; https://github.com/mapbox/react-native-mapbox-gl/blob/master/API.md#annotations
(defn marker
  "return a minimal hash-map with all required information by mapbox
  for an annotation"
  ([coord title] ; a single annotation per point in space is allowed
   {:coordinates coord ;; [latitude longitude]
    :type "point"
    :title title
    :id (str coord)}) ; 1 marker per lat/lon pair
  ([coord title subtitle]
   {:coordinates coord
    :type "point"
    :title title
    :subtitle subtitle
    :id (str coord)}))

(defn polyline
  "return a minimal hash-map with all required information by mapbox
  for an annotation"
  ([coords] ; a single annotation per point in space is allowed
   {:coordinates coords
    :type        "polyline"
    :strokeColor "#3bb2d0" ;; light
    :strokeWidth 4
    :strokeAlpha 0.5 ;; opacity
    :id          (str (first coords) (last coords))})) ; 1 marker per lat/lon pair

(defn ->verbose-coords
  "returns a {:latitude v1 :longitude v2} map. Accepts either a geojson feature
  or a Point as input"
  [geojson]
  (condp = (:type geojson)
    "Point" {:latitude (second (:coordinates geojson)) :longitude (first (:coordinates geojson))}
    "Feature" (->verbose-coords (:geometry geojson))))

(defn verbose->feature
  "takes a verbose geometrical location and unbundles it into a geojson feature.
  The following rules are applied:
  - if the object contains an id. It is assigned to the feature
  - if the object contains a bbox. It is assigned to the feature
  - any parameter besides id, latitude and longitude are wrapped as properties

  Example:
  {latitude: 0, longitude: 0, zoomLevel: 0, direction: 0, pitch: 0, animated: false}
  GeoJSON: {type: feature, geometry: {type: point, coordinates: [0, 0]}, properties: {zoomLevelL 0 ...}}
  "
  [object]
  (let [native (js->clj object :keywordize-keys true)
        lat    (:latitude native)
        lon    (:longitude native)
        extras (set/difference (set (keys native)) #{:latitude :longitude})]
    (geojson/feature "Point" [lon lat] (select-keys native extras))))

(defn annotation->feature
  "takes a mapbox annotation and unbundles it into a geojson feature.
  The following rules are applied:
  - if the object contains an id. It is assigned to the feature
  - if the object contains a bbox. It is assigned to the feature
  - any parameter besides id, bbox and coordinates are wrapped as properties

  Example: TODO
  "
  [object]
  (let [coords (reverse (:coordinates object))
        extras (set/difference (set (keys object)) #{:coordinates})]
    (condp = (:type object)
      "point"    (geojson/feature "Point" coords (select-keys object extras))
      "polyline" (geojson/feature "LineString" coords (select-keys object extras))
      "polygon"  (geojson/feature "Polygon" coords (select-keys object extras)))))

(defn feature->verbose
  "takes a feature geojson and flattens it to be a map of
  {:coordinates v1 :type v2 :id v3 :other v5 :more v6 ...}

  The following rules are applied:
  - if the feature contains an id. It is assigned to the map
  - if the feature contains a bbox. It is assigned to the map
  - all parameters in properties are put on the map

  Example:
  GeoJSON: {type: feature, geometry: {type: point, coordinates: [0, 0]}, properties: {zoomLevelL 0 ...}}
  {coordinates [0 0] type point, zoomLevel: 0, direction: 0, pitch: 0, animated: false}
  "
  [feature]
  (let [coords (:coordinates (:geometry feature))
        props  (:properties feature)
        roots  (select-keys feature [:id :bbox])]
    (merge {:coordinates coords} props roots {:type (:type (:geometry feature))})))
