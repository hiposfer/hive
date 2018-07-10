(ns hive.rework.tx
  "A reagent reactive wrapper around a datascript transactor

  Although this is a rewrite of mpdairy/posh library it is much
  more useful and simple. The problem with posh is that it tries
  to do too much. It target both datomic and datascript. Therefore
  it recreates the reagent/atom implementation internally.

  This bring other problems with it:
  - all queries must be cached (but reagent is capable of doing that
     with reagent/track)
  - whenever the state changes, posh must figure out which query result
    should be updated (but reagent is capable of doing that by diffing
     the result of the queries)
  - only the currently viewable components's queries should be executed
     (but reagent already does that since all innactive reagent/track are
     removed automatically)
  - all the problems above also make posh a big library with hundreds of lines
    (reagent.tx has barely 50 even with all datascript functionality !!)

    See https://reagent-project.github.io/news/news060-alpha.html
    for more information"
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

(defn q*
  "same as datascript/q but takes a reagent/atom as connection.
  Useful to use with reagent/track"
  [query ratom inputs]
  (apply data/q query @ratom inputs))

(defn pull*
  "same as datascript/pull but takes a reagent/atom as connection.
   Useful to use with reagent/track"
  [ratom selector eid]
  (data/pull @ratom selector eid))
