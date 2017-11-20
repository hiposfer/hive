(ns hive.queries)

(def route-stack '[:find ?stack .
                   :where [_ :route/stack ?stack]])

;; get name geometry and bbox of each city in the db
(def cities '[:find ?name ?geometry ?bbox
              :where [?entity :name]
              [?entity :bbox ?bbox]
              [?entity :name ?name]
              [?entity :geometry ?geometry]])
