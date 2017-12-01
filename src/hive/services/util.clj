(ns hive.services.util
  (:require [clojure.core.async :as async]))

(defn- throw-err [e]
  (if (instance? js/Error e) (throw e)
    e))

(defmacro <?
  "takes the value from an async channel and throws it if it is a Js Error
  Useful for receiving side Error Handling instead of producing side

  http://martintrojer.github.io/clojure/2014/03/09/working-with-coreasync-exceptions-in-go-blocks"
  [ch]
  `(throw-err (async/<! ~ch)))
