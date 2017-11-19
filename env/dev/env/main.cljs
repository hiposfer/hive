(ns ^:figwheel-no-load env.main
  (:require [reagent.core :as r]
            [hive.core :as core]
            [figwheel.client :as figwheel :include-macros true]
            [env.dev]
            [com.stuartsierra.component :as component]))

;; the system state holder
;; use only in development
(defonce app (atom nil))
(enable-console-print!)
;; init
(reset! app (core/system))
;; start
(reset! app (component/start @app))

;; initialization required by fighwheel bridge to play well with
;; react native. Please omit the ugliness of this
(def counter (r/atom 0))
(defn reloader [] @counter [(:ui (:root-ui @app))])
(def root-el (r/as-element [reloader]))

(figwheel/start {:websocket-url (str "ws://" env.dev/ip ":3449/figwheel-ws")
                 :heads-up-display false
                 :jsload-callback #(swap! counter inc)})