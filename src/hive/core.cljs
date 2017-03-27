(ns hive.core
  (:require [clojure.spec :as s]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::state (s/keys :req-un [::greeting]))

;; initial state of app-db
(def state {:map/center {:latitude 50.087641 :longitude 8.645181}
            :map/zoom 12
            ; 1 marker per lat/lon pair
            :map/annotations [{:coordinates [50.087641, 8.645181]
                               :type "point"
                               :tytle "casa"
                               :id (str [50.087641, 8.645181])}]})
