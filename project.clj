(defproject hive "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "LGPLv3"
            :url  "https://raw.githubusercontent.com/carocad/phone.app.hive/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 ;; FIXME: https://github.com/bhauman/lein-figwheel/issues/540
                 [org.clojure/clojurescript "1.9.198"]
                 [reagent "0.6.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server]]
                 [re-frame "0.8.0"]]
  :plugins [[lein-cljsbuild "1.1.4"]]
            ;[lein-figwheel "0.5.9"]]
  :clean-targets ["target/" "index.ios.js" "index.android.js"]
  :aliases {"prod-build" ^{:doc "Recompile code with prod profile."}
            ["do" "clean"
             ["with-profile" "prod" "cljsbuild" "once"]]}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.8"] ;;FIXME: https://github.com/drapanjanas/re-natal/issues/99
                                  [com.cemerick/piggieback "0.2.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths ["src" "env/dev" "script"]
                   :cljsbuild    {:builds [{:id           "ios"
                                            :source-paths ["src" "env/dev"]
                                            :figwheel     true
                                            :compiler     {:output-to     "target/ios/not-used.js"
                                                           :main          "env.ios.main"
                                                           :output-dir    "target/ios"
                                                           :optimizations :none}}
                                           {:id           "android"
                                            :source-paths ["src" "env/dev"]
                                            :figwheel     true
                                            :compiler     {:output-to     "target/android/not-used.js"
                                                           :main          "env.android.main"
                                                           :output-dir    "target/android"
                                                           :optimizations :none}}]}}
                                                           ;;FIXME: https://github.com/bhauman/lein-figwheel/issues/50
                                                           ;;  https://clojurescript.org/guides/externs
                                                           ;:infer-externs true}}]}
             :prod {:cljsbuild {:builds [{:id           "ios"
                                          :source-paths ["src" "env/prod"]
                                          :compiler     {:output-to     "index.ios.js"
                                                         :main          "env.ios.main"
                                                         :output-dir    "target/ios"
                                                         :static-fns    true
                                                         :optimize-constants true
                                                         :optimizations :simple
                                                         :closure-defines {"goog.DEBUG" false}}}
                                                         ;:infer-externs true}}
                                         {:id            "android"
                                          :source-paths ["src" "env/prod"]
                                          :compiler     {:output-to     "index.android.js"
                                                         :main          "env.android.main"
                                                         :output-dir    "target/android"
                                                         :static-fns    true
                                                         :optimize-constants true
                                                         :optimizations :simple
                                                         :closure-defines {"goog.DEBUG" false}}}]}}})
                                                         ;:infer-externs true}}]}}})
