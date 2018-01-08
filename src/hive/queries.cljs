(ns hive.queries)

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

(def user-city '[:find (pull ?city [*]) .
                 :where [_ :user/city ?city]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])

;; TODO: get the actual route
(def route '[:find ?city
             :where [_ :user/city ?city]])

(def mapbox-token '[:find ?token .
                    :where [_ :token/mapbox ?token]])
