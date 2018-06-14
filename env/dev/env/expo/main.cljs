(ns ^:figwheel-no-load env.expo.main
  (:require [cljs.spec.alpha :as s]
            [expound.alpha :as expound]
            [figwheel.client :as figwheel :include-macros true]
            [reagent.core :as r]
            [hive.core :as core]
            [env.dev]))

(enable-console-print!)
;; Setting *explain-out* does not work correctly in ClojureScript versions prior
;; to 1.9.562 due to differences in explain-data
(set! s/*explain-out* (expound/custom-printer {:print-specs? false :theme :figwheel-theme}))

;; initialization required by fighwheel bridge to play well with
;; react native. Please omit the ugliness of this
;; TODO: I am not entirely sure if this initialization is not causing me troubles
;; when I start the app, the map is shown with a warning of not providing an initial
;; region. I think that figwheel is loading the UI even before that I register it in
;; init!. If so then this glitch should only happen in development
(def counter (r/atom 0))
(defn reloader [] @counter [core/RootUi])
(def root-el (r/as-element [reloader]))

(figwheel/watch-and-reload
  :websocket-url (str "ws://" env.dev/ip ":3449/figwheel-ws")
  :heads-up-display false
  :jsload-callback #(swap! counter inc))

;; TODO: https://github.com/seantempesta/cljs-react-navigation/issues/7
;;(st/instrument)
(core/init!)
