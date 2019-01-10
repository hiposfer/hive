(ns hive.state.middleware.logger
  (:require [datascript.core :as data]
            [hive.utils.miscelaneous :as tool]
            [clojure.walk :as walk]))

(defn munge-transaction
  "replaces not printable objects with custom ones for better debugging"
  [request-id value]
  (cond
    (tool/promise? value)
    {:type "promise" :status "pending" :id request-id}

    (data/db? value)
    "#DB{...}"

    (delay? value)
    {:type "delayed effect" :value (pr-str value) :id request-id}

    (fn? value)
    (.-name value)

    :else value))

(defn- link-promise!
  "takes a promise and the id of the transaction that created it and
  logs its result on success/failure together with its id"
  [origin-id promise]
  (.. promise
      (then (fn [result]
              (let [textable (walk/postwalk #(munge-transaction origin-id %)
                                            result)]
                (js/console.log #js {:type   "promise"
                                     :status "resolved"
                                     :id     origin-id
                                     :value  (pr-str textable)}))
              (identity result)))
      (catch (fn [error] (js/console.warn #js {:type   "promise"
                                               :id     origin-id
                                               :status "rejected"
                                               :error  error})))))

(defn logger
  "returns a reducing function that will call rf after logging its arguments"
  [rf]
  (fn [db transaction]
    (when (true? js/__DEV__)
      ;; build a loggable Javascript object by replacing not printable object
      ;; with informative placeholders
      (let [id    (data/squuid)
            items (walk/postwalk #(munge-transaction id %) transaction)]
        (js/console.log #js {:id id :items items})
        (doseq [item transaction
                :when (tool/promise? item)]
          (link-promise! id item))))
    (rf db transaction)))
