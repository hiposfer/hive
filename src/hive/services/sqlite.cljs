(ns hive.services.sqlite
  "provides a service to synchronize Datascript changes to SQLite using only
  datoms as data structure i.e. E A V T -> Entity Attribute Value Transaction"
  (:require [hive.foreigns :as fl]
            [datascript.core :as data]
            [cljs.reader :as edn]
            [hive.state :as state]))

(def create-table (str "create table if not exists datoms ("
                         "id integer primary key autoincrement, "
                         "e integer, "
                         "a text, "
                         "v text, "
                         "tx integer);"))

;; NOTE: datascript reports a transaction as a deletion plus an addition
;;       for simplicity we will also do it that way and hope that we dont run
;;       out of ids :)
;(def update-datoms (str "update datoms set v = ?, tx = ? where e = ? and a = ?;"))

(def insert-datom (str "insert into datoms (e, a, v, tx) values (?, ?, ?, ?);"))

(def delete-datom (str "delete from datoms where e = ? and a = ? and v = ?;"))

(def get-all-datoms "select * from datoms;")

(def delete-all-datoms "delete from datoms;")

(def entity-store (set (for [[k v] state/schema
                             :when (and (= :store/entity (:store.type v))
                                        (contains? v :db.unique))]
                         k)))

(def ignore (set (for [[k v] state/schema
                       :when (= :store/secure (:store.type v))]
                   k)))

(defn- sync?
  "should datom d be persisted in sql. A datom is persisted
  if the db schema has an entity with :store/entity and the
  datom attribute is not set as :store/secure"
  [d tx-report]
  (let [db     (:db-after tx-report)
        datoms (filter (comp #{(:e d)} :e) (data/datoms db :eavt))]
    (cond
      (contains? ignore (:a d)) false
      (some #(contains? entity-store (:a %)) datoms) true)))

(defn sync
  "returns a sequence of sqlite transactions according to Datascript tx-report"
  [tx-report]
  (for [d (:tx-data tx-report)
        :when (sync? d tx-report)]
    (if (:added d)
      [insert-datom [(:e d) (pr-str (:a d)) (pr-str (:v d)) (:tx d)]]
      [delete-datom [(:e d) (pr-str (:a d)) (pr-str (:v d))]])))

(defn- transact!
  "executes a sequence of transactions to sync sqlite with datascript"
  [transaction tx-report]
  (doseq [[tx values] (sync tx-report)]
    ;(println tx values)
    (. transaction (executeSql tx (clj->js values)))))

(defn listen!
  "listen for datascript changes and synchronize them"
  [conn]
  (let [db (. fl/Expo (SQLite.openDatabase "sync"))]
    (. db (transaction (fn [transaction] (. transaction (executeSql create-table)))))
                       ;println println))
    (data/listen! conn
                  ::sync
                 (fn [tx-report] (. db (transaction #(transact! % tx-report)))))))

(defn- datoms
  "helper function to parse the result of a read all transaction"
  [_ result-set]
  (let [rows (js->clj (.. result-set -rows -_array)
                      :keywordize-keys true)]
    (for [r rows]
      (data/datom (:e r)
                  (edn/read-string (:a r))
                  (edn/read-string (:v r))
                  (:tx r)))))

(defn read!
  "read all datoms from the sqlite storage.

  Returns a Promise that will resolve with a sequence of datoms"
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
  "delete all datoms from the datoms table.

  Returns a promise that will resolve to a sequence of datoms"
  ^js/Promise
  []
  (let [db (. fl/Expo (SQLite.openDatabase "sync"))]
    (new js/Promise
      (fn [resolve reject]
        (. db (transaction (fn [t] (. t (executeSql delete-all-datoms #js [])))
                           reject
                           resolve))))))

;(.. (CLEAR!!)
;    (then println)
;    (catch println))

;(.. (read!)
;    (then println))
