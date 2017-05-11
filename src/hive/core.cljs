(ns hive.core
  (:require [clojure.spec :as s]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::state (s/keys :req-un [::greeting]))

;; initial state of app-db
(def state {:user/location     nil
            :map/annotations   []
            ;TODO make cities a geojson
            :user/city         {:name    "Frankfurt am Main" :region "Hessen"
                                :country "Deutschland" :short_code "de"
                                :bbox    [8.472715, 50.01552, 8.800496, 50.2269512]
                                :center  {:longitude 8.67972 :latitude 50.11361}}
            :view.home/targets false ; whether or not to display those places to the user
            :view/side-menu    false
            :view/screen       :home
            :map/ref           nil}) ;; holds a reference to the mapview instance from mapbox

;;FIXME: this shouold come from the server, not being hardcoded
(def cities
  {:de/frankfurt {:name    "Frankfurt am Main" :region "Hessen"
                  :country "Deutschland" :short_code "de"
                  :bbox    [8.472715, 50.01552, 8.800496, 50.2269512]
                  :center {:longitude 8.67972 :latitude 50.11361}}
   :de/offenburg {:name    "Offenburg" :region "Baden-Württemberg"
                  :country "Deutschland" :short_code "de"
                  :bbox    [7.8617341 48.396773 8.028459 48.55269]
                  :center  {:longitude 7.94083 :latitude 48.47083}}
   :de/freiburg  {:name    "Freiburg" :region "Baden-Württemberg"
                  :country "Deutschland" :short_code "de"
                  :bbox    [7.6620758 47.903611 7.930815 48.070923]
                  :center {:longitude 7.84972 :latitude 47.99472}}})

(def default-zoom 12)