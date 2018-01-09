(ns hive.queries)

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])

(def user-city '[:find (pull ?city [*]) .
                 :where [?id :user/id]
                        [?id :user/city ?city]])

(def user-route '[:find ?directions .
                  :where [?id :user/id]
                         [?id :user/directions ?directions]])

(def user-places '[:find ?places .
                   :where [?id :user/id]
                          [?id  :user/places ?places]])

(def user-goal '[:find ?goal .
                 :where [?id :user/id]
                        [?id :user/goal ?goal]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def mapbox-token '[:find ?token .
                    :where [_ :token/mapbox ?token]])
