(ns hive.geojson
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.set :as set]))

;;
;; GeoJSON as Clojure Spec
;; https://tools.ietf.org/html/rfc7946
;;

(s/def :geojson/lat (s/and number? #(<= -90 % 90)))
(s/def :geojson/lon (s/and number? #(<= -180 % 180)))
(s/def :geojson/position (s/or :2d (s/cat :lon :geojson/lon :lat :geojson/lat)
                               :3d (s/cat :lon :geojson/lon :lat :geojson/lat :height number?)))
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
(s/def :multiline/coordinates (s/coll-of :linestring/coordinates))
(s/def :polygon/coordinates (s/coll-of :geojson/linear-ring))
(s/def :multipolygon/coordinates (s/coll-of :polygon/coordinates))

;; --------------- geometry objects
(s/def :geojson/point ;;              "GeoJSON Point"
  (s/keys :req-un [:point/type :point/coordinates]
          :opt-un [:geojson/bbox]))

(s/def :geojson/multipoint ;  "GeoJSON MultiPoint"
  (s/keys :req-un [:multipoint/type :multipoint/coordinates]
          :opt-un [:geojson/bbox]))

(s/def :geojson/linestring
  (s/keys :req-un [:linestring/type :linestring/coordinates]
          :opt-un [:geojson/bbox]))

(s/def :geojson/multiline
  (s/keys :req-un [:multiline/type :multiline/coordinates]
          :opt-un [:geojson/bbox]))

(s/def :geojson/polygon
  (s/keys :req-un [:polygon/type :polygon/coordinates]
          :opt-un [:geojson/bbox]))

(s/def :geojson/multipolygon
  (s/keys :req-un [:multipolygon/type :multipolygon/coordinates]
          :opt-un [:geojson/bbox]))

(s/def :geojson/object (s/or :point      :geojson/point      :multipoint   :geojson/multipoint
                             :linestring :geojson/linestring :multiline    :geojson/multiline
                             :polygon    :geojson/polygon    :multipolygon :geojson/multipolygon
                             :collection :geojson/geometry-collection))

;TODO
;FeatureCollection and Geometry objects, respectively, MUST
;NOT contain a "geometry" or "properties" member.
(s/def :geocoll/geometries (s/coll-of :geojson/object))
(s/def :feature/geometry (s/nilable :geojson/object))
(s/def :featurecoll/features (s/coll-of :geojson/feature))

(s/def :feature/id (s/or :string string? :number number?))


;; -------- features/collections

(s/def :geojson/geometry-collection
  (s/keys :req-un [:geocoll/type :geocoll/geometries]))

(s/def :geojson/feature
  (s/keys :req-un [:feature/type :feature/geometry]
          :opt-un [:feature/id :geojson/bbox]))

(s/def :geojson/feature-collection
  (s/keys :req-un [:featurecoll/type :featurecoll/features]
          :opt-un [:geojson/bbox]))


;; -------------- utility functions

(defn limited-feature
  "returns an feature spec that conforms only to the specified geometry type
  instead of any geometry object"
  [geo-spec]
  (s/keys :req-un [:feature/type geo-spec]
          :opt-un [:feature/id :geojson/bbox]))

(defn- bounds
  "computes a bounding box with [min-lon, min-lat, max-lon, max-lat]"
  [coordinates]
  (let [lons (map first coordinates)
        lats (map second coordinates)]
    [(apply min lons) (apply min lats)
     (apply max lons) (apply max lats)]))

(defn- super-bounds
  "computes the bounding box of bounding boxes"
  [boxes]
  [(apply min (map #(nth % 0) boxes)) (apply min (map #(nth % 1) boxes))
   (apply max (map #(nth % 2) boxes)) (apply max (map #(nth % 3) boxes))])

;The value of the bbox member MUST be an array of
;length 2*n where n is the number of dimensions represented in the
;contained geometries, with all axes of the most southwesterly point
;followed by all axes of the more northeasterly point.
(defn bbox
  "Returns the :bbox present in the geojson object. Otherwise computes a bounding
   box with [min-lon, min-lat, max-lon, max-lat]. Note: according to the rfc7946 the values of
   a bbox array are [west, south, east, north], not [minx, miny, maxx, maxy]

   However for simplicity we calculate them that way"
  [geojson]
  (if (:bbox geojson) (:bbox geojson)
    (case (:type geojson)
      "MultiPoint" (bounds (:coordinates geojson))
      "LineString" (bounds (:coordinates geojson))
      "MultiLineString" (super-bounds (map bounds (:coordinates geojson)))
      "Polygon"         (super-bounds (map bounds (:coordinates geojson)))
      "MultiPolygon"    (super-bounds (map super-bounds (map bounds (:coordinates geojson))))
      "GeometryCollection" (let [points (filter (comp #{"Point"} :type) (:geometries geojson))
                                 others (remove (comp #{"Point"} :type) (:geometries geojson))]
                             (super-bounds (concat (map bbox others) ;; duplicate point coordinates to fake bounds
                                                   (map #(vec (concat % %)) (map :coordinates points)))))
      "Feature"    (bbox (:geometry geojson))
      "FeatureCollection" (let [geometries (map :geometry (:features geojson))
                                points     (filter (comp #{"Point"} :type) geometries)
                                others     (remove (comp #{"Point"} :type) geometries)]
                            (super-bounds (concat (map bbox others) ;; duplicate point coordinates to fake bounds
                                                  (map #(vec (concat % %)) (map :coordinates points))))))))
;(bbox {:type "FeatureCollection"
;       :features [{:type "Feature"
;                   :geometry {:type "Point" :coordinates [1 2]}}
;                  {:type "Feature"
;                   :geometry {:type "Point" :coordinates [3 4]}}]})

      ;; Point is not supported
;;FIXME: I think the bbox implementation of GeometryCollection and FeatureCollection is wrong.
;; it should take into account single points as well. Probably it would be better to compute the bbox
;; of the other elements and then expand the bbox if necessary to include the points

(defn uri
  "takes a point or feature and concatenates the coordinates as {longitude},{latitude}"
  [geojson]
  (case (:type geojson)
    "Point" (str/join "," (:coordinates geojson))
    "Feature" (uri (:geometry geojson))))

(defn geo-uri
  "return a geoUri as specified by the rfc7946. Example:
  'geo' URI:
   geo:lat,lon
   GeoJSON:{\"type\": \"Point\", \"coordinates\": [lon, lat]}\n"
  [geojson]
  (str "geo:" (uri geojson)))

(defn feature
  "returns a geojson feature with data as properties except for bbox and id"
  [inner-type coords data]
  (let [roots  (select-keys data [:id :bbox])
        extras (set/difference (set (keys data)) #{:id :bbox})]
    (merge {:type "Feature" :geometry {:type inner-type :coordinates coords}
            :properties (select-keys data extras)}
           roots)))