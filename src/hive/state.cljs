(ns hive.state)

;;FIXME: this should come from the server, not being hardcoded
(def cities (js/require "./assets/cities.json"))

(defonce schema {:user/id {:db.unique :db.unique/identity}
                 :user/name {}
                 :user/age {}
                 :user/parent {:db.valueType :db.type/ref
                               :db.cardinality :db.cardinality/many}})

;(def defaults
;  (merge
;    {:app/msg "Hello World"
;     :user/city         {:name     "Frankfurt am Main" :region "Hessen"
;                         :country  "Deutschland" :short_code "de"
;                         :bbox     [8.472715, 50.01552, 8.800496, 50.2269512]
;                         :type     "Feature"
;                         :geometry {:type "Point" :coordinates [8.67972 50.11361]}}
;     :app/cities cities}))

(def defaults
  [{:user/id "1"
    :user/name "alice"
    :user/age 27}
   {:user/id "2"
    :user/name "bob"
    :user/age 29}
   {:user/id "3"
    :user/name "kim"
    :user/age 2
    :user/parent [[:user/id "1"]
                  [:user/id "2"]]}
   {:user/id "4"
    :user/name "aaron"
    :user/age 61}
   {:user/id "5"
    :user/name "john"
    :user/age 39
    :user/parent [[:user/id "4"]]}
   {:user/id "6"
    :user/name "mark"
    :user/age 34}
   {:user/id "7"
    :user/name "kris"
    :user/age 8
    :user/parent [[:user/id "4"]
                  [:user/id "5"]]}])
