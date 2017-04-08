(ns hive.core
  (:require [clojure.spec :as s]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::state (s/keys :req-un [::greeting]))

;; initial state of app-db
(def state {:user/location {:latitude 50.087641 :longitude 8.645181} ;;FIXME
            :user/targets []
            :view/targets false ; whether or not to display those places to the user
            :map/center {:latitude 50.087641 :longitude 8.645181}
            :map/zoom    12})
