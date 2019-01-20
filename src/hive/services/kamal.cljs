(ns hive.services.kamal
  (:require [clojure.string :as str]
            [cljs.tools.reader.edn :as edn]
            [hive.state.queries :as queries]
            [datascript.core :as data]
            [lambdaisland.uri :as uri]
            [hive.utils.miscelaneous :as misc])
  (:import (goog.date DateTime)))

(def readers {'uuid uuid})

(defn zoned-time
  "returns a compatible Java LocalDateTime string representation"
  ([]
   (zoned-time (js/Date.now)))
  ([millistamp]
   (let [dtime (new DateTime.fromTimestamp millistamp)
         gtime (. dtime (toIsoString true true))]
     (str/replace gtime " " "T"))))

;(def template "https://hive-6c54a.appspot.com/directions/v5")
(def server (uri/uri "https://kamal-live.herokuapp.com/"))
(def templates
  {:area/directions ["area" ::area "directions"] ;?coordinates={coordinates}&departure={departure}
   :area/entity     ["area" ::area ::entity ::id]
   :area/meta       ["area" ::area]
   :kamal/areas     ["area"]})

(defn- query-string
  [m]
  (str/join "&" (for [[k v] m] (str k "=" (js/encodeURIComponent v)))))

(defn- path
  [k values]
  (let [template (get templates k)]
    (str "/" (str/join "/" (replace values template)))))

(defn- read-text [^js/Response response] (. response (text)))

(defn- parse-edn [text] (edn/read-string {:readers readers} text))

(defn entity
  "ref is a map with a single key value pair of the form {:trip/id 2}"
  [db ref]
  (let [[k v]    (first ref)
        area-id  (data/q queries/user-area-id db)
        resource (path :area/entity
                       {::area area-id
                        ::entity (namespace k)
                        ::id v})
        url      (assoc server :path resource)]
    [(str url) {:method  "GET"
                :headers {:Accept "application/edn"}}]))

(defn get-entity!
  "executes the result of entity with js/fetch.

  Returns a promise that will resolve to a transaction with the
  requested entity"
  [db ref]
  ;; TODO: dont request if entity already exists in db
  (let [[url opts] (entity db ref)]
    (.. (js/fetch url (clj->js opts))
        (then (fn [^js/Response response] (. response (text))))
        (then #(edn/read-string {:readers readers} %))
        (then vector))))

(defn- chain!
  "request a remote entity and also fetches the UNIQUE entity under
  keyword attribute.

  For example: fetch the trip/id 123 and then the :trip/route that it
  points to"
  [db trip-ref attribute]
  (.. (get-entity! db trip-ref)
      (then (fn [[trip]] [trip [get-entity! db (get trip attribute)]]))))

(defn directions
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [db coordinates departure]
  (let [area-id    (data/q queries/user-area-id db)
        query      (query-string {"coordinates" coordinates
                                  ;;"departure"   (zoned-time departure)
                                  "departure"   "2018-05-07T10:15:30+01:00"})
        url        (assoc server :path (path :area/directions
                                             {::area area-id})
                                 :query query)]
    [(str url) {:method  "GET"
                :headers {:Accept "application/edn"}}]))

(defn get-directions!
  "executes the result of directions with js/fetch.

  Returns a transaction that will resolve to a transaction that assigns the
  returned route to the current user.

  All gtfs trips and route are also requested"
  ^js/Promise
  ([db coordinates departure]
   (let [[url opts] (directions db coordinates (zoned-time departure))]
     (.. (js/fetch url (clj->js opts))
         (then misc/on-fetch-response)
         (then #(edn/read-string {:readers readers} %)))))
  ([db coordinates]
   (get-directions! db coordinates (js/Date.now))))

(defn get-areas!
  "fetches the supported areas from kamal"
  []
  (let [resource (path :kamal/areas {})
        uri      (str (assoc server :path resource))]
    (.. (js/fetch uri)
        (then read-text)
        (then parse-edn))))
