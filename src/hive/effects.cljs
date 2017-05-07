(ns hive.effects
  (:require [re-frame.core :as rf]
            [re-frame.router :as router]
            [hive.foreigns :as fl]))

(defonce debounces (atom {}))
;(defonce throttles (atom {})) ;; FIXME

;; https://github.com/Day8/re-frame/issues/233#issuecomment-252739068
(defn debounce
  [[id event-vec n]]
  (js/clearTimeout (@debounces id))
  (swap! debounces assoc id
         (js/setTimeout (fn [] (rf/dispatch event-vec)
                               (swap! debounces dissoc id))
                        n)))

(defn res->text [res] (.text res))
(defn res->json [res] (.json res))

(defn fetch
  [url opts on-response process-response]
  (-> (js/fetch url opts)
      (.then on-response)
      (.then process-response)
      (.catch #(println %))))

(defn fetch-json
  [[url options handler]]
  (fetch url options res->json handler))

(defn box-map
  "modify the mapview to display the area specified in the parameters"
  [[map-ref [latSW lngSW latNE lngNE] padding]]
  (let [[padTop padRight padDown padLeft] (or padding [100 100 100 100])]
    (when map-ref
      (.setVisibleCoordinateBounds map-ref
                                   latSW lngSW latNE lngNE
                                   padTop padRight padDown padLeft))))

(defn center&zoom
  [[map-ref lat lng zoom]]
  (.setCenterCoordinateZoomLevel map-ref lat lng zoom))

(defn quit
  "quits the android app"
  [v]
  (.exitApp fl/back-android))

;(fetch "https://google.com" {} (cons res->json [#(println %)]))