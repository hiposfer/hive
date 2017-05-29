(ns hive.core
  (:require [clojure.spec :as s]
            [hive.geojson :as geojson]))

;; -- Spec --------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec
;;
;; The value in app-db should always match this spec. Only event handlers
;; can change the value in app-db so, after each event handler
;; has run, we re-check app-db for correctness (compliance with the Schema).
;;
;; How is this done? Look in events.cljs and you'll notice that all handers
;; have an "after" interceptor which does the spec re-check.
;;
;; None of this is strictly necessary. It could be omitted. But we find it
;; good practice.

(s/def :user/location (s/nilable (geojson/limited-feature :geojson/point)))
(s/def :map/annotations (s/coll-of (s/or :point      (geojson/limited-feature :geojson/point)
                                         :linestring (geojson/limited-feature :geojson/linestring)
                                         :polygon    (geojson/limited-feature :geojson/polygon))))
(s/def :user/city (geojson/limited-feature :geojson/point))
(s/def :view.home/targets boolean?)
(s/def :view/side-menu boolean?)
(s/def :view/screen keyword?) ;; do we need more than this?
(s/def :map/ref (s/nilable any?));; js object with custom constructor
;; spec of app-db
(s/def :hive/state (s/keys :req [:map/ref
                                 :user/location :user/city
                                 :view/side-menu :view/screen :view.home/targets]))

;; initial state of app-db
(def state {:user/location     nil
            :map/annotations   []
            :user/city         {:name    "Frankfurt am Main" :region "Hessen"
                                :country "Deutschland" :short_code "de"
                                :bbox    [8.472715, 50.01552, 8.800496, 50.2269512]
                                :type    "Feature"
                                :geometry {:type "Point" :coordinates [8.67972 50.11361]}}
            :view.home/targets false ; whether or not to display those places to the user
            :view/side-menu    false
            :view/screen       :blockade
            :map/ref           nil}) ;; holds a reference to the mapview instance from mapbox

;;FIXME: this should come from the server, not being hardcoded
(def cities
 {:de/frankfurt {:name    "Frankfurt am Main" :region "Hessen"
                 :country "Deutschland" :short_code "de"
                 :bbox    [8.472715, 50.01552, 8.800496, 50.2269512]
                 :type    "Feature"
                 :geometry {:type "Point" :coordinates [8.67972 50.11361]}}
  :de/offenburg {:name    "Offenburg" :region "Baden-Württemberg"
                 :country "Deutschland" :short_code "de"
                 :bbox    [7.8617341 48.396773 8.028459 48.55269]
                 :type    "Feature"
                 :geometry {:type "Point" :coordinates [7.94083 48.47083]}}
  :de/freiburg  {:name    "Freiburg" :region "Baden-Württemberg"
                 :country "Deutschland" :short_code "de"
                 :bbox    [7.6620758 47.903611 7.930815 48.070923]
                 :type    "Feature"
                 :geometry {:type "Point" :coordinates [7.84972 47.99472]}}})

(def default-zoom 12)