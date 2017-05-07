(ns hive.util
  "functions that come handy every now and then")

;; MapBox annotations
;; https://github.com/mapbox/react-native-mapbox-gl/blob/master/API.md#annotations
(defn mark
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

(defn route
  "return a minimal hash-map with all required information by mapbox
  for an annotation"
  ([coords] ; a single annotation per point in space is allowed
   {:coordinates coords
    :type        "polyline"
    :strokeColor "#3bb2d0" ;; light
    :strokeWidth 4
    :strokeAlpha 0.5 ;; opacity
    :id          (str (first coords) (last coords))})) ; 1 marker per lat/lon pair
