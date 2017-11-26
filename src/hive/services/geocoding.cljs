(ns hive.services.geocoding
  (:require [cljs.core.async :refer-macros [go go-loop]]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]
            [hive.services.http :as http]
            [hive.rework.core :as rework]))

(s/def ::coordinate (s/tuple number? number?))
(s/def ::query (s/or :location string?
                     :coordinate ::coordinate))
(s/def ::mode #{"mapbox.places" "mapbox.places-permanent"})
(s/def ::country string?)
(s/def ::proximity ::coordinate)
(s/def ::types string?)
(s/def ::autocomplete boolean?)
(s/def ::bbox (s/tuple number? number? number? number?))
(s/def ::limit number?)
(s/def ::language string?)

(s/def ::request (s/keys :req [::query ::http/success ::mode]
                         :opt [::http/error ::country ::proximity ::types
                               ::autocomplete ::bbox ::limit ::language]))

(def template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}")

(defn- handle!
  [request http]
  (let [params  (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v)))
                     (dissoc request ::query ::mode))
        URL (-> (str/replace template "{mode}" (::mode request))
                (str/replace "{query}" (js/encodeURIComponent (::query request)))
                (str/replace "{params}" (str/join "&" params)))]
    (go (async/>! http (assoc request ::http/json URL)))))

;; ---------------------------------
(defrecord Service [http config chan]
  component/Lifecycle
  (start [this]
    (if-not (nil? chan) this
      (let [chan (async/chan 3)]
        (go-loop [_ nil]
          (let [request (async/<! chan)]
            (if (nil? request) nil ;; stops looping
              (if (s/valid? ::request request)
                (recur (handle! (assoc request ::access_token (:mapbox config))
                                (:chan http)))
                (recur (s/explain ::request request))))))
        (assoc this :chan chan))))
  (stop [this]
    (async/close! chan)
    (assoc this :chan nil)))

(defn autocomplete!
  "takes an autocomplete geocoding channel and a request shaped
   according to MapBox geocode API v5 and executes it asynchronously

  https://www.mapbox.com/api-documentation/#request-format"
  [channel request] ;; we double check here becuase query can be valid for reverse geocoding
  (go (async/>! channel (assoc request ::autocomplete true))))

;(let [success (async/chan)]
;  (go (rework/using ::service autocomplete! {::query "bruchfeldplatz 7a"
;                                             ::http/success success
;                                             ::mode "mapbox.places"})
;      (println (async/<! success))))