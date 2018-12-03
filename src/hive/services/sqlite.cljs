(ns hive.services.sqlite
  "provides a service to synchronize Datascript changes to SQLite using only
  datoms as data structure i.e. E A V T -> Entity Attribute Value Transaction"
  (:require [expo :as Expo]
            [datascript.core :as data]
            [cljs.reader :as edn]
            [hive.state.core :as state]))

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

(defn- storable-entity?
  [entity]
  (reduce-kv (fn [_ k v]
               (when (and (= :sqlite/store (get v :hive.storage))
                          (= :db.unique/identity (get v :db.unique))
                          (some? (get entity k)))
                 (reduced true)))
             nil
             state/schema))

(defn- sync?
  "a datom is persisted if the db schema has an entity with :sqlite/store
   and the attribute is not marked as :sqlite/ignore.

   Datoms that references other datoms are only stored if both the datom and
   its reference are marked for storage"
  [datom tx-report]
  (let [db      (:db-after tx-report)
        entity  (data/entity db (:e datom))
        storage (get-in state/schema [(:a datom) :hive.storage])
        _type   (get-in state/schema [(:a datom) :db.valueType])]
    (cond
      ;; attribute explicitly marked as ignore - do not sync
      (= :sqlite/ignore storage)
      false

      ;; attribute explicitly marked as store
      (= :sqlite/store storage)
      true

      ;; attribute value references an entity marked for storage
      ;; and the attribute itself is marked for storage
      (and (storable-entity? (get entity (:a datom)))
           (storable-entity? entity)
           (= :db.type/ref _type))
      true

      ;; attribute belongs to an entity marked for storage
      (and (storable-entity? entity)
           (not= :db.type/ref _type))
      true

      ;; ignore by default
      :else false)))

(defn- transact!
  "executes a sequence of transactions to sync sqlite with datascript"
  [transaction tx-report]
  (doseq [d (:tx-data tx-report)
          :when (sync? d tx-report)
          :let [values    [(:e d) (pr-str (:a d)) (pr-str (:v d)) (:tx d)]
                statement (if (:added d)
                            insert-datom
                            delete-datom)]]
    (println d)
    (. transaction (executeSql statement (clj->js values)))))

(defn listen!
  "listen for datascript changes and synchronize them"
  [conn]
  (let [db (Expo/SQLite.openDatabase "sync")]
    (. db (transaction (fn [transaction]
                         (. transaction (executeSql create-table)))))
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

(defn init!
  "creates a sqlite table if it doesnt exists and reads all datoms from storage.

  Returns a Promise that will resolve with a sequence of datoms"
  ^js/Promise
  []
  (let [db (Expo/SQLite.openDatabase "sync")]
    (new js/Promise
      (fn [resolve reject]
        (. db (transaction (fn [t]
                             (. t (executeSql create-table))
                             (. t (executeSql get-all-datoms
                                              #js []
                                              #(resolve (datoms %1 %2))
                                              #(reject %2))))
                           reject))))))
            ;; success

(defn clear!
  "deletes all datoms from the database.

  Returns a promise that will resolve to a sequence of datoms"
  ^js/Promise
  []
  (let [db (Expo/SQLite.openDatabase "sync")]
    (new js/Promise
      (fn [resolve reject]
        (. db (transaction (fn [t] (. t (executeSql delete-all-datoms #js [])))
                           reject
                           resolve))))))

;(.. (CLEAR!!)
;    (then println)
;    (catch println))

#_(.. (init!)
      (then println))
