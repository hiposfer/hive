(ns hive.services.kamal
  (:require [clojure.string :as str]
            [cljs.tools.reader.edn :as edn])
  (:import (goog.date DateTime Interval)))

(def readers {'uuid uuid})

(defn- zoned-time
  "returns a compatible Java LocalDateTime string representation"
  ([]
   (zoned-time (new DateTime)))
  ([^js/DateTime now]
   (let [gtime (. now (toIsoString true true))]
     (str/replace gtime " " "T"))))

;(def template "https://hive-6c54a.appspot.com/directions/v5")
(def server "http://try.hiposfer.com")
(def urls
  {:area/directions "{server}/area/frankfurt/directions?coordinates={coordinates}&departure={departure}"
   :area/entity     "{server}/area/frankfurt/{entity}/{id}"
   :area/meta       "{server}/area/frankfurt"
   :kamal/areas     "{server}/area"})

(defn- fill-out
  "fills out the entries in the provided string, using the keys as match"
  [template entries]
  (reduce-kv (fn [res k v] (str/replace res k v))
             template
             entries))

(defn- read-text [^js/Response response] (. response (text)))

(defn- parse-edn [text] (edn/read-string {:readers readers} text))

(defn entity
  "ref is a map with a single key value pair of the form {:trip/id 2}"
  [ref]
  (let [[k v] (first ref)
        url   (fill-out (:area/entity urls) {"{entity}" (namespace k)
                                             "{id}"     v
                                             "{server}" server})]
    [url {:method  "GET"
          :headers {:Accept "application/edn"}}]))

(defn get-entity!
  "executes the result of entity with js/fetch.

  Returns a promise that will resolve to a transaction with the
  requested entity
  "
  [ref]
  ;; TODO: dont request if entity already exists in db
  (let [[url opts] (entity ref)]
    (.. (js/fetch url (clj->js opts))
        (then (fn [^js/Response response] (. response (text))))
        (then #(edn/read-string {:readers readers} %))
        (then vector))))
;; TODO: error handling)

(defn- chain!
  "request an remote entity and also fetches the entity under keyword k
  when it arrives.

  For example: fetch the trip/id 123 and then the :trip/route that it
  points to"
  [trip-ref k]
  (.. (get-entity! trip-ref)
      (then (fn [[trip]] [trip [get-entity! (k trip)]]))))

(defn process-directions
  "takes a kamal directions response and attaches it to the current user.
  Further trip information is also retrieved"
  [path user]
  (let [base [path
              {:user/uid        user
               :user/directions [:directions/uuid (:directions/uuid path)]}]]
    (concat base
      (distinct
        (for [step (:directions/steps path)
              :when (= (:step/mode step) "transit")
              ;; check just in case ;)
              :when (some? (:step/trip step))]
          [chain! (:step/trip step) :trip/route])))))


(defn directions
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [coordinates departure]
  (let [ztime (js/encodeURIComponent (zoned-time departure))
        url   (fill-out (:area/directions urls)
                        {"{coordinates}" coordinates
                         "{departure}"   ztime
                         "{server}" server})] ;; "2018-05-07T10:15:30+01:00"))]
    [url {:method  "GET"
          :headers {:Accept "application/edn"}}]))

(defn get-directions!
  "executes the result of directions with js/fetch.

  Returns a transaction that will resolve to a transaction that assigns the
  returned route to the current user.

  All gtfs trips and route are also requested"
  ^js/Promise
  ([coordinates user departure]
   (let [[url opts] (directions coordinates departure)]
     (.. (js/fetch url (clj->js opts))
         (then (fn [^js/Response response] (. response (text))))
         (then #(edn/read-string {:readers readers} %))
         (then #(process-directions % user)))))
  ;; TODO: error handling
  ([coordinates user]
   (get-directions! coordinates user (new DateTime))))

(defn get-areas!
  "fetches the supported areas from kamal"
  []
  (.. (js/fetch (fill-out (:kamal/areas urls) {"{server}" server}))
      (then read-text)
      (then parse-edn)))
