(defproject hive "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "LGPLv3"
            :url  "https://raw.githubusercontent.com/carocad/phone.app.hive/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671"]
                 ;; FIXME: there is a problem with the inclusion of createClass into reagent
                 ;; https://github.com/reagent-project/reagent/blob/master/CHANGELOG.md#070-2762017
                 [reagent "0.6.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server]]
                 [re-frame "0.9.4"]
                 [expound "0.1.1"]]
  :plugins [[lein-cljsbuild "1.1.6"]]
            ;[lein-figwheel "0.5.9"]]
  :global-vars {*warn-on-infer* true
                *print-length* 50}
  :clean-targets ["target/" "index.ios.js" "index.android.js"]
  :aliases {"prod-build" ^{:doc "Recompile code with prod profile."}
            ["do" "clean"
             ["with-profile" "prod" "cljsbuild" "once"]]}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.11"]
                                  [com.cemerick/piggieback "0.2.2"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths ["src" "env/dev" "script"]
                   :cljsbuild    {:builds [{:id           "ios"
                                            :source-paths ["src" "env/dev"]
                                            :figwheel     true
                                            :compiler     {:output-to     "target/ios/not-used.js"
                                                           :main          "env.ios.main"
                                                           :output-dir    "target/ios"
                                                           :optimizations :none
                                                           :infer-externs true
                                                           :source-map true}}
                                           {:id           "android"
                                            :source-paths ["src" "env/dev"]
                                            :figwheel     true
                                            :compiler     {:output-to     "target/android/not-used.js"
                                                           :main          "env.android.main"
                                                           :output-dir    "target/android"
                                                           :optimizations :none
                                                           :infer-externs true
                                                           :source-map true}}]}}
             :prod {:cljsbuild {:builds [{:id           "ios"
                                          :source-paths ["src" "env/prod"]
                                          :compiler     {:output-to     "index.ios.js"
                                                         :main          "env.ios.main"
                                                         :output-dir    "target/ios"
                                                         :static-fns    true
                                                         :optimize-constants true
                                                         :optimizations :simple
                                                         :closure-defines {"goog.DEBUG" false}
                                                         :infer-externs true}}
                                         {:id            "android"
                                          :source-paths ["src" "env/prod"]
                                          :compiler     {:output-to     "index.android.js"
                                                         :main          "env.android.main"
                                                         :output-dir    "target/android"
                                                         :static-fns    true
                                                         :optimize-constants true
                                                         :optimizations :simple
                                                         :closure-defines {"goog.DEBUG" false}
                                                         :infer-externs true}}]}}})
