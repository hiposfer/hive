(ns hive.effects
  (:require [re-frame.core :as rf]
            [re-frame.router :as router]
            [hive.foreigns :as fl]
            [hive.util :as util]
            [hive.geojson :as geojson]))

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

(defn retrieve!
  "wrapper around React Native fetch function
  See https://facebook.github.io/react-native/docs/network.html
  for more information"
  [url opts on-response process-response]
  (-> (js/fetch url opts)
      (.then on-response)
      (.then process-response)
      (.catch #(println %))))

(defn retrieve->json!
  [[url options handler]]
  (retrieve! url options res->json handler))

(defn quit!
  "quits the android app"
  [v]
  (.exitApp fl/back-android))

;(retrieve "https://google.com" {} (cons res->json [#(println %)]))