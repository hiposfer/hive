(ns hive.effects
  (:require [re-frame.router :as router]
            [hive.foreigns :as fl]
            [hive.core :as hive]))

;; fixme: save tokens once received

(def init-json-url "https://firebasestorage.googleapis.com/v0/b/hive-6c54a.appspot.com/o/app%2Finit.json?alt=media&token=03675ebe-dc51-4ff8-8e80-f8fefdda2757")

(defn init [_ _]
  {:db hive/state
   :app.internet/enabled? :app/internet
   :fetch/json [init-json-url {} :hive/services]})

(defonce debounces (atom {}))
;(defonce throttles (atom {})) ;; FIXME

;; https://github.com/Day8/re-frame/issues/233#issuecomment-252739068
(defn debounce
  [[id event-vec n]]
  (js/clearTimeout (@debounces id))
  (swap! debounces assoc id
         (js/setTimeout (fn [] (router/dispatch event-vec)
                               (swap! debounces dissoc id))
                        n)))

(defn res->text [res] (.text res))
(defn res->json [res] (.json res))

(defn retrieve!
  "wrapper around React Native fetch function
  See https://facebook.github.io/react-native/docs/network.html
  for more information"
  [url opts on-response event-id]
  (if (nil? event-id) (throw (ex-info (str "ERROR: no event-id given to process the result of "
                                           url)
                                      {}))
    (-> (js/fetch url opts)
        (.then on-response)
        (.then #(router/dispatch [event-id %]))
        (.catch #(println "ERROR: fetch failed for " url "\nmessage" %)))))

(defn retrieve->json!
  [[url options handler-id]]
  (retrieve! url options res->json handler-id))

(defn quit!
  "quits the android app"
  [_]
  (.exitApp fl/back-handler))

;(retrieve "https://google.com" {} (cons res->json [#(println %)]))

(defn show-toast!
  "Display an Android Toast text with the specified duration or
   SHORT otherwise"
  [[text duration]]
  (.show fl/toast-android text (or duration fl/toast-android.SHORT)))

(defn clear-search-box!
  "clears the text value of a NativeBase input text"
  [input-ref]
  (when (and (exists? input-ref) (not (nil? input-ref))) ;; root needed due to wrapping
    (.clear (.-_root input-ref))))

(defn get-internet-state
  "fetches a boolean indicating whether or the app has access to internet"
  [handler]
  (-> (.-isConnected fl/net-info)
      (.fetch)
      (.then #(router/dispatch [handler %]))))