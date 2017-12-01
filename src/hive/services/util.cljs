(ns hive.services.util)

(defn chan?
  [x]
  (satisfies? cljs.core.async.impl.protocols/Channel x))
