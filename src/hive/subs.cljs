(ns hive.subs)

(defn get-rf
  "basic subscription handler. It gets the event id from the in-memorz db"
  [db [id]] (get db id))