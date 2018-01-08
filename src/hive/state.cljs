(ns hive.state)

;;FIXME: this should come from the server, not being hardcoded
(def cities (js->clj (js/require "./assets/cities.json")
              :keywordize-keys true))
(def tokens (js->clj (js/require "./assets/init.json")
              :keywordize-keys true))

(def schema {:user/city {:db.valueType     :db.type/ref
                         :db.cardinality   :db.cardinality/one}
             :user/id {:db.unique :db.unique/identity}
             ;:route/stack {:db.valueType   :db.type/string
             ;              :db.cardinality :db.cardinality/many}
             :city/name {:db.unique :db.unique/identity}})

(defn with-ns
  "modify a map keys to be namespaced with ns"
  [ns m]
  (zipmap (map #(keyword ns %) (keys m))
          (vals m)))

;; needs to be an array of maps. This will be used for data/transact!
(def defaults
  (concat (map #(with-ns "city" %) cities)
          [{:user/id -1 ;; dummy
            :user/city [:city/name "Frankfurt am Main"]}
           (with-ns "token" tokens)]))

