(ns hive.services.directions
  (:require [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [hive.services.raw.http :as http]
            [hive.rework.core :as rework]
            [hive.queries :as queries]
            [hive.rework.util :as tool]))

(s/def ::input (s/and string? not-empty))
(s/def ::coordinates (s/tuple number? number?))
(s/def ::alternatives boolean?)
(s/def ::geometries #{"geojson" "polyline" "polyline6"})
(s/def ::overview #{"simplified" "full" "false"})
(s/def ::radiuses (s/coll-of (s/or :inf "unlimited" :num pos?)))
(s/def ::steps boolean?)
(s/def ::language ::input)
(s/def ::profile #{"mapbox/driving-traffic" "mapbox/driving"
                   "mapbox/walking" "mapbox/cycling"})
(s/def ::access_token (s/and string? not-empty))

(s/def ::request (s/keys :req [::profile ::coordinates ::access_token]
                         :opt [::language ::steps ::radiuses ::overview
                               ::geometries  ::alternatives]))

(def template "https://api.mapbox.com/directions/v5/{profile}/{coordinates}?{params}")

(defn- request
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service"
  [request]
  (let [coords  (str/join ";" (map #(str/join "," %) (::coordinates request)))
        request (assoc request ::geometries "geojson"
                               ::steps true
                               ::profile "mapbox/walking"
                               ::coordinates coords)
        params  (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v)))
                     (dissoc request ::coordinates ::profile))
        URL (-> (str/replace template "{profile}" (::profile request))
                (str/replace "{coordinates}" (js/encodeURIComponent (::coordinates request)))
                (str/replace "{params}" (str/join "&" params)))]
    [URL]))

(def request!
  "takes an autocomplete geocoding channel and a request shaped
   according to MapBox geocode API v5 and executes it asynchronously.
   Returns a channel with the result or an exception

  https://www.mapbox.com/api-documentation/#request-format"
  (rework/pipe (rework/inject ::access_token queries/mapbox-token)
               request
               http/json!
               tool/keywordize))

(s/fdef request :args (s/cat :request ::request))
