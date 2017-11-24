(ns hive.rework.reagent.tx
  (:require [datascript.core :as data]
            [reagent.core :as r]))


(defn listen!
  "registers a listener for the connection transactions. Returns a
  reagent/ratom whose value will automatically updated on every
  transact"
  [conn]
  (let [ratom (r/atom @conn)]
    (data/listen! conn ::tx (fn [tx-report] (reset! ratom (:db-after tx-report))))
    (swap! conn assoc ::ratom ratom)))

(defn unlisten!
  "unregisters the transaction listener previously attached with
  listen!"
  [conn]
  (data/unlisten! conn ::tx)
  (swap! conn dissoc ::ratom))

(defn- q*
  [query ratom inputs]
  (apply data/q query @ratom inputs))

(defn q
  "same as datascript/q but returns a ratom which will be updated
  every time that the value of conn changes. It takes a connection
  not a value database"
  [query conn & inputs]
  (r/track q* query (::ratom @conn) inputs))

(defn- pull*
  [ratom selector eid]
  (data/pull @ratom selector eid))

(defn pull
  "same as datascript/pull but returns a ratom which will be updated
  every time that the value of conn changes. It takes a connection
  not a value database"
  [conn selector eid]
  (r/track pull* (::ratom @conn) selector eid))

(defn- entity*
  [ratom eid]
  (data/entity @ratom eid))

(defn entity
  "same as datascript/entity but returns a ratom which will be updated
  every time that the value of conn changes. It takes a connection
  not a value database"
  [conn eid]
  (r/track (::ratom @conn) eid))