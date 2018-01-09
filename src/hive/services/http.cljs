(ns hive.services.http
  (:require [cljs.core.async :as async]
            [cljs.spec.alpha :as s]
            [hive.rework.core :as rework]
            [hive.rework.util :as tool]))

(s/def ::init (s/map-of keyword? any?))
(s/def ::url (s/and string? not-empty))
(s/def ::request (s/cat :URL ::url :options (s/? ::init)))

(defn- request!
  "takes an http channel and a request shaped according to
  spec's ::request and executes it asynchronously. Extra http
  properties can be passed as per fetch documentation.
  Returns a channel with the result of fetch, an exception on error
  or a Promise if ::http/raw is chosen. Throws on invalid request

  https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch"
  [[url init]]
  (js/fetch url (clj->js init)))

(defn- json
  [promise]
  (let [result (async/chan)]
    (-> promise
        (.then #(.json %))
        (.then #(async/put! result %))
        (.catch #(async/put! result (ex-info "network error" %
                                             ::network-error))))
    result))

(defn- text
  [promise]
  (let [result (async/chan)]
    (-> promise
        (.then #(.text %))
        (.then #(async/put! result %))
        (.catch #(async/put! result (ex-info "network error" %
                                             ::network-error))))
    result))

(def json! (rework/pipe request!
                        json))

(def text! (rework/pipe request!
                        text))

(s/fdef request! :args (s/cat :request ::request))
