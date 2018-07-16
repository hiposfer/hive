(ns hive.services.kamal
  (:require [clojure.string :as str])
  (:import (goog.date DateTime)))

(defn- local-time
  "returns a compatible Java LocalDateTime string representation"
  ([]
   (local-time (new DateTime)))
  ([^js/DateTime now]
   (let [gtime (.. now (toIsoString true))]
     (str/replace gtime " " "T"))))

;(def template "https://hive-6c54a.appspot.com/directions/v5")
(def template "http://192.168.0.45:3000/area/frankfurt/directions?coordinates={coordinates}&departure={departure}")

(defn directions
  "takes a map with the items required by ::request and replaces their values into
   the Mapbox URL template. Returns the full url to use with an http service

   https://www.mapbox.com/api-documentation/#request-format"
  [coordinates departure]
  (let [url (-> (str/replace template "{coordinates}" coordinates)
                (str/replace "{departure}" (local-time departure)))]
    [url {:method "GET"
          :headers {:Accept "application/json"}}]))

(defn directions!
  "executes the result of directions with js/fetch.

  Returns a Promise with a Clojure data structure.

  Rejects when status not= Ok"
  ^js/Promise
  [coordinates departure]
  (let [[url opts] (directions coordinates departure)]
    (.. (js/fetch url (clj->js opts))
        (then (fn [^js/Response response] (. response (json))))
        (then (fn [result]
                (if (= (goog.object/get result "code") "Ok")
                  (js->clj result :keywordize-keys true)
                  (js/Promise.reject (ex-info (goog.object/get "msg") result))))))))
