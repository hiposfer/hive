(ns hive.services.http
  (:require [cljs.core.async :as async]
            [cljs.spec.alpha :as s]))

(s/def ::json string?)
(s/def ::raw string?)
(s/def ::text string?)
(s/def ::request (s/keys :req [(or ::json ::raw ::text)]
                         :opt [::error]))

(def http-types #{::json ::text ::raw})


;; TODO: make raw, json and text be pipes
(defn request!
  "takes an http channel and a request shaped according to
  spec's ::request and executes it asynchronously. Extra http
  properties can be passed as per fetch documentation.
  Returns a channel with the result of fetch, an exception on error
  or a Promise if ::http/raw is chosen. Throws on invalid request

  https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch"
  [request]
  (let [result    (async/chan)
        command   (select-keys request http-types)
        [res url] (first command)
        promise   (-> (js/fetch url (clj->js request))
                      (.catch #(async/put! result %)))
        report    #(async/put! result %)]
    (case res
      ::raw  (-> promise (.then report))
      ::json (-> promise (.then #(.json %))
                         (.then report))
      ::text (-> promise (.then #(.text %))
                         (.then report)))
    result))

(s/fdef request! :args (s/cat :request ::request))
