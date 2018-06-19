(ns hive.services.directions
  (:require [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [hiposfer.geojson.specs :as geojson]))

;(s/def ::coordinates (s/tuple number? number?))
;(s/def ::alternatives boolean?)
;(s/def ::geometries #{"geojson" "polyline" "polyline6"})
;(s/def ::overview #{"simplified" "full" "false"})
(s/def ::radiuses (s/coll-of (s/or :inf "unlimited" :num pos?)))
(s/def ::steps boolean?)
;(s/def ::language ::input)
;(s/def ::profile #{"mapbox/driving-traffic" "mapbox/driving"
;                   "mapbox/walking" "mapbox/cycling")
;(s/def ::access_token (s/and string? not-empty))

(s/def ::request (s/keys :req-un [:hiposfer.geojson.specs.multipoint/coordinates]
                         :opt-un [::steps ::radiuses]))

;(def template "https://hive-6c54a.appspot.com/directions/v5")
(def template "http://192.168.0.45:3000/directions/v5")

(defn request
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [request]
  [template {:method "POST"
             :body (js/JSON.stringify (clj->js {:arguments request}))
             :headers {:Accept "application/json",
                       :Content-Type "application/json"}}])

(s/fdef request :args (s/cat :request ::request))
