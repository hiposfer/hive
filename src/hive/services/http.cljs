(ns hive.services.http
  (:require [cljs.core.async :refer-macros [go go-loop]]
            [cljs.core.async :as async]
            [com.stuartsierra.component :as component]
            [cljs.spec.alpha :as s]))

(defn- chan?
  [x]
  (satisfies? cljs.core.async.impl.protocols/Channel x))


(s/def ::json string?)
(s/def ::raw string?)
(s/def ::text string?)
(s/def ::success chan?)
(s/def ::error chan?)
(s/def ::request (s/keys :req [(or ::json ::raw ::text) ::success]
                         :opt [::error]))

(def http-types #{::json ::text ::raw})

(defn handle-http!
  [request]
  (let [command   (select-keys request http-types)
        success   (get request ::success)
        error     (get request ::error)
        [res url] (first command)
        promise   (js/fetch url (clj->js request))
        promise   (if-not error promise
                    (.catch promise #(go (async/>! error %))))
        report    #(go (->> (js->clj % :keywordize-keys true)
                            (async/>! success)))]
    (case res
      ::raw  (-> promise (.then #(go (async/>! success %))))
      ::json (-> promise (.then #(.json %))
                         (.then report))
      ::text (-> promise (.then #(.text %))
                         (.then report)))))
      ;(cljs.pprint/pprint request))))

;; ---------------------------------
(defrecord Service [chann]
  component/Lifecycle
  (start [this]
    (if-not (nil? chann) this
      (let [chann (async/chan 10)]
        (go-loop [_ nil]
         (let [request (async/<! chann)]
           (if (nil? request) nil ;; stops looping
             (if (s/valid? ::request request)
               (recur (handle-http! request))
               (recur (s/explain ::request request))))))
        (assoc this :chan chann))))
  (stop [this]
    (async/close! chann)
    (assoc this :chan nil)))

(defn request!
  "takes an http channel and a request shaped according to
  spec's ::request and executes it asynchronously. Extra http
  properties can be passed as per fetch documentation:

  https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch"
  [channel request]
  (go (async/>! channel request)))