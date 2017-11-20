(ns hive.state)

;;FIXME: this should come from the server, not being hardcoded
(def cities (js->clj (js/require "./assets/cities.json")
              :keywordize-keys true))

(def schema {:user/city {:db.valueType     :db.type/ref
                         :db.cardinality   :db.cardinality/one}
             :user/id {:db.unique :db.unique/identity}
             ;:route/stack {:db.valueType   :db.type/string
             ;              :db.cardinality :db.cardinality/many}
             :city/name {:db.unique :db.unique/identity}})

;; needs to be an array of maps. This will be used for data/transact!
(def defaults
  (concat (sequence (comp (map #(assoc % :city/name (:name %)))
                         (map #(dissoc % :name)))
                    cities)
          [{:user/id -1 ;; dummy
            :user/city [:city/name "Frankfurt am Main"]}]))
