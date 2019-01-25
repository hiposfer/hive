(ns hive.utils.promises
  "utility functions to get better integration of promises with transact!"
  (:require [cljs.core.async :as async]))

(defn guard
  "guards the execution of an effect promise with a catch statement that will
  return a transaction on [{:error/id error-id}] with the information from the
  error.

  Promotes all errors to Clojure's ex-info"
  [effect error-id]
  (let [[f & args] effect]
    (.. (apply f args)
        (catch
          (fn [error]
            (if (instance? ExceptionInfo error)
              [{:error/id error-id :error/info error}]
              [{:error/id error-id :error/info (ex-info (ex-message error)
                                                        error)}]))))))

(defn finally
  "regardless of the success or result of effect, passes it to f;
   prepending it to the arguments"
  [effect [f & args]]
  (let [[fe & fe-args] effect]
    (.. (apply fe fe-args)
        (then (fn [result] (apply f (cons result args))))
        (catch (fn [error] (apply f (cons error args)))))))

(defn async
  "takes a promise and returns a promise channel that can be used for asynchronous
  coordination with core.async"
  [promise]
  (let [result (async/promise-chan)]
    (.. promise
        (then (fn [value] (async/put! result value)))
        (catch (fn [error] (async/put! result error))))
    result))
