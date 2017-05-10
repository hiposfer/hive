(ns hive.geojson
  (:require [cljs.spec :as s]))

;;
;; GeoJSON as Clojure Spec
;; https://tools.ietf.org/html/rfc7946
;;

(s/def :geojson/lat (s/and number? #(<= -90 % 90)))
(s/def :geojson/lon (s/and number? #(<= -180 % 180)))
(s/def :geojson/position (s/or (s/cat :lat :geojson/lat :lon :geojson/lon)
                               (s/cat :lat :geojson/lat :lon :geojson/lon :height number?)))
(s/def :geojson/properties (s/map-of keyword? any?))
(s/def :geojson/linear-ring (s/and (s/coll-of :geojson/position :min-count 4)
                                   #(= (first %) (last %))))
(s/def :geojson/bbox (s/cat :west number? :south number?
                            :east number? :north number?))

(s/def :point/type (s/and string? #(= "Point" %)))
(s/def :multipoint/type (s/and string? #(= "MultiPoint" %)))
(s/def :linestring/type (s/and string? #(= "LineString" %)))
(s/def :multiline/type (s/and string? #(= "MultiLineString" %)))
(s/def :polygon/type (s/and string? #(= "Polygon" %)))
(s/def :multipolygon/type (s/and string? #(= "MultiPolygon" %)))
(s/def :geocoll/type (s/and string? #(= "GeometryCollection" %)))
(s/def :feature/type (s/and string? #(= "Feature" %)))
(s/def :featurecoll/type (s/and string? #(= "FeatureCollection" %)))

(s/def :point/coordinates :geojson/position)
(s/def :multipoint/coordinates (s/coll-of :geojson/position))
(s/def :linestring/coordinates (s/coll-of :geojson/position :min-count 2))
(s/def :multiline/coordinates (s/coll-of :geojson/line-coords))
(s/def :polygon/coordinates (s/coll-of :geojson/linear-ring))
(s/def :multipolygon/coordinates (s/coll-of :geojson/polygon-coords))

(s/def :geocoll/geometries (s/coll-of :geojson/object))
(s/def :feature/geometry (s/nilable :geojson/object))
(s/def :featurecoll/features (s/coll-of :geojson/feature))

(s/def :feature/id (s/or :string string? :number number?))

;; --------------- geometry objects
(s/def :geojson/point ;;              "GeoJSON Point"
  (s/keys :req-un [:point/type :point/coordinates]
          :opt-un [:geojson/properties :geojson/bbox]))

(s/def :geojson/multipoint ;  "GeoJSON MultiPoint"
  (s/keys :req-un [:multipoint/type :multipoint/coordinates]
          :opt-un [:geojson/properties :geojson/bbox]))

(s/def :geojson/linestring
  (s/keys :req-un [:linestring/type :linestring/coordinates]
          :opt-un [:geojson/properties :geojson/bbox]))

(s/def :geojson/multiline
  (s/keys :req-un [:multiline/type :multiline/coordinates]
          :opt-un [:geojson/properties :geojson/bbox]))

(s/def :geojson/polygon
  (s/keys :req-un [:polygon/type :polygon/coordinates]
          :opt-un [:geojson/properties :geojson/bbox]))

(s/def :geojson/multipolygon
  (s/keys :req-un [:multipolygon/type :multipolygon/coordinates]
          :opt-un [:geojson/properties :geojson/bbox]))

(s/def :geojson/object (s/or :point :geojson/point :multipoint :geojson/multipoint
                             :linestring :geojson/linestring :multiline :geojson/multiline
                             :polygon :geojson/polygon :multipolygon :geojson/multipolygon
                             :collection :geojson/geometry-collection))

;; -------- features/collections

(s/def :geojson/geometry-collection
  (s/keys :req-un [:geocoll/type :geocoll/geometries]))

(s/def :geojson/feature
  (s/keys :req-un [:feature/type :feature/geometry]
          :opt-un [:feature/id :geojson/bbox]))

(s/def :geojson/feature-collection
  (s/keys :req-un [:featurecoll/type :featurecoll/features]
          :opt-un [:geojson/bbox]))