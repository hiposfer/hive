(ns hive.queries
  (:require [hive.rework.core :as work]))

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])

(def user-city '[:find (pull ?city [*]) .
                 :where [?id :user/id]
                        [?id :user/city ?city]])

(def user-directions '[:find ?directions .
                       :where [?id :user/id]
                              [?id :user/directions ?directions]])

(def user-places '[:find ?places .
                   :where [?id :user/id]
                          [?id :user/places ?places]])

(def user-goal '[:find ?goal .
                 :where [?id :user/id]
                        [?id :user/goal ?goal]])

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