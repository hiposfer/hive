(ns hive.state)

;;FIXME: this should come from the server, not being hardcoded
(def cities (js/require "./assets/cities.json"))

(def defaults
  (merge
    {:app/msg "Hello World"
     :user/city         {:name     "Frankfurt am Main" :region "Hessen"
                         :country  "Deutschland" :short_code "de"
                         :bbox     [8.472715, 50.01552, 8.800496, 50.2269512]
                         :type     "Feature"
                         :geometry {:type "Point" :coordinates [8.67972 50.11361]}}
     :app/cities cities}))