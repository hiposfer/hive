(ns hive.queries
  (:require [hive.rework.core :as work]))

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])

(def city-info
  "returns the user city, directions, places and goal"
  '[:find (pull ?id [:user/city :user/directions
                     :user/places :user/goal]) .
    :where [?id :user/id]])

(def user-directions '[:find (pull ?directions [*]) .
                       :where [?id :user/id]
                       [?id :user/directions ?directions]])

(def user-position '[:find ?position .
                     :where [?id :user/id]
                            [?id :user/position ?position]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def mapbox-token '[:find ?token .
                    :where [_ :token/mapbox ?token]])

(def session '[:find ?session .
               :where [_ :app/session ?session]])

;(work/q '{:find [(pull ?city [*]) .]
;          :where [[?id :user/id]
;                  [?id :user/city ?city]]})
