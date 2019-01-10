(ns hive.state.logger
  (:require [datascript.core :as data]
            [hive.utils.miscelaneous :as tool]
            [clojure.walk :as walk]))

(defn- demunge-effect
  "replaces function objects with their Javascript names and Datascript
  instances with a tag to avoid overflowing ouput"
  [[f & args]]
  (into [(.-name f)]
        (walk/postwalk (fn [v]
                         (cond
                           (data/db? v)
                           "#DB{...}"

                           (and (vector? v) (fn? (first v)))
                           (into [(.-name (first v))] (rest v))

                           :else v))
                       args)))

(defn object
  "returns a map which can be used for logging on a Javascript console"
  [request-id value]
  (cond
    ;; JS promise - wait for its value then transact it
    (tool/promise? value)
    (do (.. value (then (fn [result]
                          (js/console.log #js {:type "promise" :status "resolved" :id request-id
                                               :value (clj->js (object request-id result))})
                          (identity result)))
            (catch (fn [error] (js/console.warn #js {:type "promise"
                                                     :id request-id
                                                     :status "rejected"
                                                     :error error}))))
        {:type "promise" :status "pending" :id request-id})

    ;; functional side effect declaration
    ;; Execute it and try to execute its result
    (and (vector? value) (fn? (first value)))
    {:type "effect" :value (pr-str (demunge-effect value)) :id request-id}

    ;; side effect declaration wrapped with delay to allow testing
    ;; Force it and try to execute its result
    (delay? value)
    {:type "native effect" :value (pr-str value) :id request-id}

    ;; transaction item
    (or (vector? value) (data/datom? value) (map? value))
    {:type "tx" :value (pr-str value) :id request-id}

    ;; datascript transaction
    (seq? value)
    {:type "transaction" :value (pr-str value) :id request-id}))
