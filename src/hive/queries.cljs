(ns hive.queries
  (:require [hive.rework.core :as work]))

;; TODO: most of these queries rely on user ID. It could be better to compute
;; that once and later on just pull or entity attributes out of it

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])

(def user-position '[:find ?position . :where [_ :user/position ?position]])

(def places-id '[:find [?id ...] :where [?id :place/id]])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def mapbox-token '[:find ?token .
                    :where [_ :ENV/MAPBOX ?token]])

(def session '[:find ?session .
               :where [_ :session/uuid ?session]])

;(work/q '{:find [(pull ?city [*]) .]
;          :where [[?id :user/id]
;                  [?id :user/city ?city]]})

(def routes-ids '[:find [?routes ...]
                  :where [_ :directions/uuid ?routes]])
