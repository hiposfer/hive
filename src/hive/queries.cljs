(ns hive.queries
  (:require [hive.rework.core :as work]))

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])

(def map-info
  "returns the map directions, places and goal"
  '[:find (pull ?uid [:user/places :user/goal :user/directions]) .
    :where [?uid :user/id]])

(def user-city '[:find (pull ?city [*]) .
                 :where [?uid :user/id]
                        [?uid :user/city ?city]])

(def user-directions '[:find (pull ?directions [*]) .
                       :where [?id :user/id]
                              [?id :user/directions ?directions]])

(def user-places '[:find ?places .
                   :where [?id :user/id]
                          [?id :user/places ?places]])

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

(work/q user-city)
