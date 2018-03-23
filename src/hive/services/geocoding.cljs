(ns hive.services.geocoding
  (:require [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [hive.services.raw.http :as http]
            [hive.rework.core :as work]
            [hive.rework.util :as tool]
            [hiposfer.geojson.specs :as geojson]))

(s/def ::input (s/and string? not-empty))
(s/def ::query (s/or :location ::input
                     :coordinate ::geojson/position))
(s/def ::mode #{"mapbox.places" "mapbox.places-permanent"})
(s/def ::country ::input)
(s/def ::proximity ::geojson/feature)
(s/def ::types string?)
(s/def ::autocomplete boolean?)
(s/def ::bbox ::geojson/bbox)
(s/def ::limit number?)
(s/def ::language ::input)
(s/def ::access_token (s/and string? not-empty))

;; artifitial constraints -  not per mapbox api
(s/def ::request (s/keys :req [::query ::proximity]
                         :opt [::mode ::access_token ::country ::bbox
                               ::types ::limit ::language ::autocomplete]))

(def template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}")

(defn defaults
  [request]
  (assoc request ::autocomplete true
                 ::mode  "mapbox.places"
                 ::proximity (geojson/uri (::proximity request))
                 ::bbox (str/join "," (::bbox request))))

(defn autocomplete
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [request]
  (let [params  (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v)))
                     (dissoc request ::query ::mode))
        URL (-> (str/replace template "{mode}" (::mode request))
                (str/replace "{query}" (js/encodeURIComponent (::query request)))
                (str/replace "{params}" (str/join "&" params)))]
      [URL]))

(s/fdef autocomplete :args (s/cat :request ::request))
