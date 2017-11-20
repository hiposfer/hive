(ns ^:figwheel-no-load env.main
  (:require [reagent.core :as r]
            [hive.core :as core]
            [hive.rework.core :as rework]
            [figwheel.client :as figwheel :include-macros true]
            [env.dev]
            [com.stuartsierra.component :as component]))

(enable-console-print!)
;; start
(rework/init! (component/start (core/system)))

;; initialization required by fighwheel bridge to play well with
;; react native. Please omit the ugliness of this
(def counter (r/atom 0))
(defn reloader [] @counter [core/root-ui])
(def root-el (r/as-element [reloader]))

(figwheel/start {:websocket-url (str "ws://" env.dev/ip ":3449/figwheel-ws")
                 :heads-up-display false
                 :jsload-callback #(swap! counter inc)})