(ns hive.services.raw.http
  (:require [cljs.core.async :as async]
            [cljs.spec.alpha :as s]
            [hive.rework.util :as tool]))

(s/def ::init (s/map-of keyword? any?))
(s/def ::url (s/and string? not-empty))
(s/def ::request (s/cat :URL ::url :options (s/? ::init)))

(defn json!
  "takes a request shaped according to ::request and executes it asynchronously.
   Extra http properties can be passed as per fetch documentation.
   Returns a channel with xform applied to its result or an exception on error

  https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch"
  [[url init] xform]
  (let [result (async/chan 1 (comp tool/bypass-error xform))]
    (-> (js/fetch url (clj->js init))
        (.then #(.json %))
        (.then #(do (async/put! result %) (async/close! result)))
        (.catch #(async/put! result %)))
    result))

(defn text!
  "takes a request shaped according to ::request and executes it asynchronously.
   Extra http properties can be passed as per fetch documentation.
   Returns a channel with xform applied to its result or an exception on error

  https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch"
  [[url init] xform]
  (let [result (async/chan 1 (comp tool/bypass-error xform))]
    (-> (js/fetch url (clj->js init))
        (.then #(.text %))
        (.then #(do (async/put! result %) (async/close! result)))
        (.catch #(async/put! result %))) ;; halt on error
    result))

(s/fdef json! :args (s/cat :request ::request :xform fn?))

(s/fdef text! :args (s/cat :request ::request :xform fn?))
