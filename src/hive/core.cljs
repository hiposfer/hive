(ns hive.core
  (:require [clojure.spec :as s]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::state (s/keys :req-un [::greeting]))

;; initial state of app-db
(def state {:user/location nil
            :user/targets []; the places the user searched for
            :user/city {:name "Frankfurt am Main" :region "Hessen"
                        :country "Deutschland" :short_code "de"
                        :bbox [8.472715, 50.01552, 8.800496, 50.2269512]}
            :view/targets false ; whether or not to display those places to the user
            :view/screen :home
            :map/ref nil ;; holds a reference to the mapview instance from mapbox
            :map/camera {:center {:latitude 50.087641 :longitude 8.645181}
                         :zoom 12}})
