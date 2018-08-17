(ns hive.services.sqlite
  "provides a service to synchronize Datascript changes to SQLite using only
  datoms as data structure i.e. E A V T -> Entity Attribute Value Transaction"
  (:require [hive.foreigns :as fl]
            [datascript.core :as data]))

(def create-table (str "create table if not exists datoms ("
                         "id integer primary key autoincrement, "
                         "e integer, "
                         "a text, "
                         "v text, "
                         "tx integer);"))

;; TODO: datascript reports a transaction as a deletion plus an addition
;; for simplicity we will also do it that way and hope that we dont run out
;; of int numbers
;(def update-datoms (str "update datoms set v = ?, tx = ? where e = ? and a = ?;"))

(def insert-datom (str "insert into datoms (e, a, v, tx) values (?, ?, ?, ?);"))

(def delete-datom (str "delete from datoms where e = ? and a = ?;"))

(def get-all-datoms "select * from datoms;")

(def delete-all-datoms "delete from datoms;")

(defn sync
  [tx-report]
  (for [d (:tx-data tx-report)]
    (if (:added d)
      [insert-datom [(:e d) (pr-str (:a d)) (pr-str (:v d)) (:tx d)]]
      [delete-datom [(:e d) (pr-str (:a d))]])))

(defn- transact!
  [transaction tx-report]
  (doseq [[tx values] (sync tx-report)]
    (. transaction (executeSql tx (clj->js values)))))

(defn sync!
  [db tx-report]
  (. db (transaction #(transact! % tx-report)))); println println)))
;(cljs.pprint/pprint changes)
;(cljs.pprint/pprint tx-report)))

(defn listen!
  [conn]
  (let [db (. fl/Expo (SQLite.openDatabase "sync"))]
    (. db (transaction (fn [transaction] (. transaction (executeSql create-table)))))
                       ;println println))
    (data/listen! conn ::sync #(sync! db %))))


(defn- datoms
  [tx result-set]
  (let [result (js->clj (.. result-set -rows -_array)
                        :keywordize-keys true)]
    (js/console.log result)
    (for [r result]
      (data/datom (:e r) (:a r) (:v r) (:tx r)))))

(defn read!
  ^js/Promise
  []
  (let [db (. fl/Expo (SQLite.openDatabase "sync"))]
    (new js/Promise
      (fn [resolve reject]
        (. db (transaction (fn [t] (. t (executeSql get-all-datoms
                                                    #js []
                                                    #(resolve (datoms %1 %2))
                                                    #(reject %2))))
                reject))))))
                ;; success

(defn CLEAR!!
  []
  (let [db (. fl/Expo (SQLite.openDatabase "sync"))]
    (new js/Promise
      (fn [resolve reject]
        (. db (transaction (fn [t] (. t (executeSql delete-all-datoms
                                                    #js []
                                                    #(resolve %2))))
                           reject))))))

;(.. (CLEAR!!)
;    (then println)
;    (catch println))

;(.. (read!)
;    (then println))
