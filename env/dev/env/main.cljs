(ns ^:figwheel-no-load env.main
  (:require [cljs.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [expound.alpha :as expound]
            [figwheel.client :as figwheel :include-macros true]
            [reagent.core :as r]
            [hive.core :as core]
            [env.dev]))


(enable-console-print!)
;; Setting *explain-out* does not work correctly in ClojureScript versions prior
;; to 1.9.562 due to differences in explain-data
(set! s/*explain-out* expound/printer)

;; TODO: https://github.com/seantempesta/cljs-react-navigation/issues/7
;;(st/instrument)
(core/init!)

;; initialization required by fighwheel bridge to play well with
;; react native. Please omit the ugliness of this
(def counter (r/atom 0))
(defn reloader [] @counter [core/root-ui])
(def root-el (r/as-element [reloader]))

(figwheel/start {:websocket-url (str "ws://" env.dev/ip ":3449/figwheel-ws")
                 :heads-up-display false
                 :jsload-callback #(swap! counter inc)})
