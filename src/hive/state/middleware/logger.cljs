(ns hive.state.middleware.logger
  (:require [datascript.core :as data]
            [hive.utils.miscelaneous :as tool]
            [clojure.walk :as walk]
            [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-pro]))

(defn munge-transaction
  "replaces not printable objects with custom ones for better debugging"
  [value]
  (cond
    (data/db? value)
    "#DB{...}"

    (delay? value)
    {:type "delayed effect" :value (pr-str value)}

    (fn? value)
    (.-name value)

    (keyword? value)
    (str value)

    (uuid? value)
    (str value)

    (data/datom? value)
    (pr-str value)

    :else value))

;; TODO: link second stage promises - those promises that
;; are created as a result of a previous promise
(defn- link-promise!
  "takes a promise and the id of the transaction that created it and
  logs its result on success/failure together with its id"
  [origin-id promise]
  (.. promise
      (then (fn [result]
              (let [tx (walk/postwalk munge-transaction result)]
                (js/console.log (str origin-id)
                                #js {:type   "promise"
                                     :status "resolved"
                                     :value  (pr-str tx)})
                (identity result))))
      (catch (fn [error] (js/console.warn (str origin-id)
                                          #js {:type   "promise"
                                               :status "rejected"
                                               :error  error})))))

(defn- link-channel
  [origin-id chan]
  (async/map (fn [value]
               (let [tx (walk/postwalk munge-transaction value)]
                 (js/console.log (str origin-id)
                                 #js {:type   "channel"
                                      :status (if (async-pro/closed? chan)
                                                "closed" "open")
                                      :value  (pr-str tx)})
                 (identity value)))
             [chan]))

(defn logger
  "returns a middleware that will call the middleware chain after logging its arguments"
  [middleware]
  (fn [db transaction]
    (when (true? js/__DEV__)
      ;; build a loggable Javascript object by replacing not printable object
      ;; with informative placeholders
      (let [id     (data/squuid)
            items  (walk/postwalk munge-transaction transaction)
            result (middleware db transaction)]
        ;; log transaction and effects
        (apply js/console.log (str id) (clj->js items))
        ;; link promises to future logs
        (doseq [item result :when (tool/promise? item)]
          (link-promise! id item))
        ;; return transaction
        (for [item result]
          (if (not (tool/channel? item))
            (identity item)
            (link-channel id item)))))))
