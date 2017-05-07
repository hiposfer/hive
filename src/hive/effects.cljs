(ns hive.effects
  (:require [re-frame.core :as rf]
            [re-frame.router :as router]))

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

;;TODO: move somewhere else. This doesnt belong in effects
(defn mark
  "return a minimal hash-map with all required information by mapbox
  for an annotation"
  ([coord title] ; a single annotation per point in space is allowed
   {:coordinates coord ;; [latitude longitude]
    :type "point"
    :title title
    :id (str coord)}) ; 1 marker per lat/lon pair
  ([coord title subtitle]
   {:coordinates coord
    :type "point"
    :title title
    :subtitle subtitle
    :id (str coord)}))

(defn route
  "return a minimal hash-map with all required information by mapbox
  for an annotation"
  ([coords] ; a single annotation per point in space is allowed
   {:coordinates coords
    :type        "polyline"
    :strokeColor "#3bb2d0" ;; light
    :strokeWidth 4
    :strokeAlpha 0.5 ;; opacity
    :id          (str (first coords) (last coords))})) ; 1 marker per lat/lon pair

;(fetch "https://google.com" {} (cons res->json [#(println %)]))

