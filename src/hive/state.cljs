(ns hive.state)

;;FIXME: this should come from the server, not being hardcoded
(def cities (js->clj (js/require "./assets/cities.json")
              :keywordize-keys true))

(def schema {:user/city {:db.valueType     :db.type/ref
                         :db.cardinality   :db.cardinality/one}
             :route/stack {:db.valueType   :db.type/string
                           :db.cardinality :db.cardinality/many}})

;; needs to be an array of maps. This will be used for data/transact!
(def defaults cities)