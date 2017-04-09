(ns hive.core
  (:require [clojure.spec :as s]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::state (s/keys :req-un [::greeting]))

;; initial state of app-db
(def state {:user/location {:latitude 50.087641 :longitude 8.645181} ;;FIXME
            :user/targets []
            :user/city {:name "Frankfurt am Main" :region "Hessen"
                        :country "Deutschland" :short_code "de"
                        :bbox [8.472715, 50.01552, 8.800496, 50.2269512]}
            :view/targets false ; whether or not to display those places to the user
            :map/center {:latitude 50.087641 :longitude 8.645181}
            :map/zoom    12})
