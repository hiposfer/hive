(ns hive.services.geocoding
  (:require [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [hive.services.http :as http]
            [hive.rework.core :as rework]
            [hive.queries :as queries]
            [hive.rework.util :as tool]))

(s/def ::input (s/and string? not-empty))
(s/def ::coordinate (s/tuple number? number?))
(s/def ::query (s/or :location ::input
                     :coordinate ::coordinate))
(s/def ::mode #{"mapbox.places" "mapbox.places-permanent"})
(s/def ::country ::input)
(s/def ::proximity ::coordinate)
(s/def ::types string?)
(s/def ::autocomplete boolean?)
(s/def ::bbox (s/tuple number? number? number? number?))
(s/def ::limit number?)
(s/def ::language ::input)
(s/def ::access_token (s/and string? not-empty))

(s/def ::request (s/keys :req [::query ::mode ::access_token]
                         :opt [::country ::proximity ::types
                               ::autocomplete ::bbox ::limit ::language]))

(def template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}")

(defn- autocomplete
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service"
  [request]
  (let [request (rework/inject request ::access_token queries/mapbox-token)
        request (assoc request ::autocomplete true)
        params  (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v)))
                     (dissoc request ::query ::mode))
        URL (-> (str/replace template "{mode}" (::mode request))
                (str/replace "{query}" (js/encodeURIComponent (::query request)))
                (str/replace "{params}" (str/join "&" params)))]
    {::http/json URL}))

(def autocomplete!
  "takes an autocomplete geocoding channel and a request shaped
   according to MapBox geocode API v5 and executes it asynchronously.
   Returns a channel with the result or an exception

  https://www.mapbox.com/api-documentation/#request-format"
  (rework/pipe #(tool/validate ::query (::query %) ::invalid-input)
               autocomplete
               http/request!
               tool/keywordize))

(s/fdef autocomplete :args (s/cat :request ::request))
