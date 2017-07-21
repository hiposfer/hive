(ns hive.subscriptions)

(defn get-rf
  "basic subscription handler. It gets the event id from the in-memory db"
  [db [id]] (get db id))