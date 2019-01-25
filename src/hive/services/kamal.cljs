(ns hive.services.kamal
  (:require [clojure.string :as str]
            [cljs.tools.reader.edn :as edn]
            [hive.state.queries :as queries]
            [datascript.core :as data]
            [lambdaisland.uri :as uri]
            [hive.utils.miscelaneous :as misc]
            [cljs.core.async :as async]
            [hive.utils.promises :as promise])
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
   :area/gtfs       ["area" ::area "gtfs"]
   :area/meta       ["area" ::area]
   :kamal/areas     ["area"]})

(defn- query-string
  [m]
  (str/join "&" (for [[k v] m] (str k "=" (js/encodeURIComponent v)))))

(defn- path
  [k values]
  (let [template (get templates k)]
    (str "/" (str/join "/" (replace values template)))))

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
  (async/go
    (let [[url opts] (entity db ref)
          response (async/<! (promise/async (js/fetch url (clj->js opts))))
          body     (async/<! (promise/async (. response (text))))]
      (edn/read-string {:readers readers} body))))

(defn directions
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [db coordinates departure]
  (let [area-id    (data/q queries/user-area-id db)
        query      (query-string {"coordinates" coordinates
                                  ;;"departure"   (zoned-time departure)})
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
  ([db coordinates]
   (get-directions! db coordinates (js/Date.now)))
  ([db coordinates departure]
   (let [result (async/chan)]
     (get-directions! db coordinates departure result)
     result))
  ([db coordinates departure chan]
   (async/go
     (let [[url opts] (directions db coordinates departure)
           response   (async/<! (promise/async (js/fetch url (clj->js opts))))
           body       (async/<! (promise/async (. response (text))))]
       (if (not (.-ok response))
         (async/>! chan [{:error/id ::directions
                          :error/info (ex-info (str "Error fetching directions: " body)
                                               (misc/roundtrip response))}])
         (let [content (edn/read-string {:readers readers} body)
               user    (data/q queries/user-id db)
               trips   (for [step (:directions/steps content)
                             :when (= "transit" (:step/mode step))
                             :when (some? (:step/trip step))]
                         (:step/trip step))]
           (async/>! chan [content {:user/uid        user
                                    :user/directions [:directions/uuid (:directions/uuid content)]}])
           (doseq [trip-id (distinct trips)]
             (let [trip  (async/<! (get-entity! db trip-id))
                   route (get-entity! db (get trip :trip/route))]
               (async/>! chan [trip])
               (async/>! chan [(async/<! route)])))))))))

(defn get-areas!
  "fetches the supported areas from kamal"
  []
  (async/go
    (let [resource (path :kamal/areas {})
          uri      (str (assoc server :path resource))
          ;; TODO: why doesnt fetch has EDN as mime type ?
          response (async/<! (promise/async (js/fetch uri)))
          body     (async/<! (promise/async (. response (text))))]
      (edn/read-string {:readers readers} body))))

(defn query-gtfs!
  ([db query]
   (query-gtfs! db query {}))
  ([db query args]
   (async/go
     (let [area-id  (data/q queries/user-area-id db)
           resource (path :area/gtfs {::area area-id})
           uri      (assoc server :path resource
                                  :query (query-string {"q" query
                                                        "args" args}))
           opts     {:method  "GET"
                     :headers {:Accept "application/edn"}}
           response (async/<! (promise/async (js/fetch (str uri (clj->js opts)))))
           body     (async/<! (promise/async (. response (text))))]
       (println (edn/read-string {:readers readers} body))))))
