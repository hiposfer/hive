(ns hive.services.geocoding
  (:require [cljs.core.async :refer-macros [go go-loop]]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [hive.services.http :as http]))

(s/def ::coordinate (s/tuple number? number?))
(s/def ::query (s/or :location string?
                     :coordinate ::coordinate))
(s/def ::mode #{"mapbox.places" "mapbox.places-permanent"})
(s/def ::country string?)
(s/def ::proximity ::coordinate)
(s/def ::types string?)
(s/def ::autocomplete boolean?)
(s/def ::bbox (s/tuple number? number? number? number?))
(s/def ::limit number?)
(s/def ::language string?)
(s/def ::access_token string?)

(s/def ::request (s/keys :req [::query ::mode ::access_token]
                         :opt [::country ::proximity ::types
                               ::autocomplete ::bbox ::limit ::language]))

(def template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}")

(defn autocomplete!
  "takes an autocomplete geocoding channel and a request shaped
   according to MapBox geocode API v5 and executes it asynchronously.
   Returns a channel with the result or an exception. Throws on invalid
   request

  https://www.mapbox.com/api-documentation/#request-format"
  [request]
  (let [request (assoc request ::autocomplete true)
        params  (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v)))
                     (dissoc request ::query ::mode))
        URL (-> (str/replace template "{mode}" (::mode request))
                (str/replace "{query}" (js/encodeURIComponent (::query request)))
                (str/replace "{params}" (str/join "&" params)))]
    (http/request! {::http/json URL})))

(s/fdef autocomplete! :args (s/cat :request ::request))

;(let [success (async/chan)]
;  (go (rework/using ::service autocomplete! {::query "bruchfeldplatz 7a"
;                                             ::http/success success
;                                             ::mode "mapbox.places"})
;      (println (async/<! success))))
