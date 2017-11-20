(ns hive.queries)

;; get name geometry and bbox of each city in the db
(def cities '[:find ?name ?geometry ?bbox ?region ?country
              :where [?entity :city/name ?name]
                     [?entity :bbox ?bbox]
                     [?entity :geometry ?geometry]
                     [?entity :region ?region]
                     [?entity :country ?country]])

(def user-city '[:find [?name ?bbox ?geometry]
                 :where [_ :user/city ?city]
                        [?city :city/name ?name]
                        [?city :bbox ?bbox]
                        [?city :geometry ?geometry]])

(def user-id '[:find ?uid .
               :where [_ :user/id ?uid]])