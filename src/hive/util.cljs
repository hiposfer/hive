(ns hive.util
  "functions that come handy every now and then"
  (:require [clojure.set :as set]
            [hive.geojson :as geojson]))

;; MapBox annotations
;; https://github.com/mapbox/react-native-mapbox-gl/blob/master/API.md#annotations
(defn polyline
  "return a minimal hash-map with all required information by mapbox
  for an annotation"
  ([feature] ; a single annotation per point in space is allowed
   (merge feature
     {:strokeColor "#3bb2d0" ;; light
      :strokeWidth 4
      :strokeAlpha 0.5 ;; opacity
      :id (str (first (:coordinates (:geometry feature)))
               (last  (:coordinates (:geometry feature))))})))

(defn annotation->feature
  "takes a mapbox annotation and unbundles it into a geojson feature.
  The following rules are applied:
  - if the object contains an id. It is assigned to the feature
  - if the object contains a bbox. It is assigned to the feature
  - any parameter besides id, bbox and coordinates are wrapped as properties

  Example: TODO
  "
  [object]
  (let [extras (set/difference (set (keys object)) #{:coordinates})]
    (case (:type object)
      "point"    (geojson/feature "Point"
                                  (reverse (:coordinates object))
                                  (select-keys object extras))
      "polyline" (geojson/feature "LineString"
                                  (map reverse (:coordinates object))
                                  (select-keys object extras))
      "polygon"  (geojson/feature "Polygon"
                                  (map reverse (:coordinates object))
                                  (select-keys object extras)))))

(defn feature->annotation
  "takes a feature geojson and flattens it to be a map of
  {:coordinates v1 :type v2 :id v3 :other v5 :more v6 ...}
  Note that according to the mapbox documentation, an annotation
  should have the shape lat lon which is the reverse of the geojson
  standard

  The following rules are applied:
  - if the feature contains an id. It is assigned to the map
  - if the feature contains a bbox. It is assigned to the map
  - all parameters in properties are put on the map

  Example:
  GeoJSON: {type: feature, geometry: {type: Point, coordinates: [5 10]}, properties: {zoomLevelL 0 ...}}
  {coordinates [10 5] type point, zoomLevel: 0, direction: 0, pitch: 0, animated: false}
  "
  [feature]
  (let [extras (set/difference (set (keys feature)) #{:id :bbox :type :properties})
        props  (merge (:properties feature) (select-keys feature extras))
        roots  (select-keys feature [:id :bbox])]
    (case (:type (:geometry feature))
      "Point"      (merge {:coordinates (reverse (:coordinates (:geometry feature)))}
                          props roots {:type "point"})
      "LineString" (merge {:coordinates (map reverse (:coordinates (:geometry feature)))}
                          props roots {:type "polyline"})
      "Polygon"    (merge {:coordinates (map reverse (:coordinates (:geometry feature)))}
                          props roots {:type "polygon"}))))

(defn feature->verbose
  "returns a {:latitude v1 :longitude v2} map. Accepts either a geojson feature
  or a Point as input"
  [geojson]
  (case (:type geojson)
    "Point" {:latitude  (second (:coordinates geojson))
             :longitude (first  (:coordinates geojson))}
    "Feature" (feature->verbose (:geometry geojson))))

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
