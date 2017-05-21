(ns hive.wrappers.mapbox
  "See https://github.com/mapbox/react-native-mapbox-gl/blob/master/API.md"
  (:require [hive.geojson :as geojson]
            [hive.foreigns :as fl]))

(defn init! [token] (.setAccessToken fl/MapBox token))

(defn box-map!
  "modify the mapview to display the area specified in the parameters"
  [[map-ref geojson]]
  (let [[min-lon, min-lat, max-lon, max-lat] (geojson/bbox geojson)
        padding                              (:padding (:properties (:geometry geojson)))
        [padTop padRight padDown padLeft]    (or padding [100 100 100 100])]
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

