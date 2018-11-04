(defproject hive "0.1.0-SNAPSHOT"
  :description "Your go-to routing app for public transport"
  :url "https://github.com/hiposfer/hive"
  :license {:name "LGPLv3"
            :url  "https://raw.githubusercontent.com/hiposfer/hive/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 ;; TODO: update to next release for better inference
                 [org.clojure/clojurescript "1.10.339"]
                 [react-native-externs "0.2.0"]
                 [org.clojure/core.async "0.3.465"]
                 [reagent "0.7.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server cljsjs/create-react-class]]
                 [datascript "0.16.6"]
                 [expound "0.7.0"]
                 [cljs-react-navigation "0.1.1"]
                 [hiposfer/geojson.specs "0.2.0"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.14"]]
  :clean-targets ["target/" "main.js"]
  :aliases {"figwheel"   ["run" "-m" "user" "--figwheel"]
            "prod-build" ["do" ["clean"]
                               ["run" "-m" "user" "--prepare-release"]
                               ["cljsbuild" "once" "release"]]}
  :profiles {:dev  {:dependencies [[figwheel-sidecar "0.5.14"]
                                   [com.cemerick/piggieback "0.2.1"]
                                   [org.clojure/test.check "0.9.0"]]
                    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :cljsbuild {:builds [{:id           "main" ;; do NOT change this - used by figwheel bridge
                        :source-paths ["src"]
                        :figwheel     true
                        :compiler     {:output-to     "target/expo/not-used.js"
                                       :main          "env.expo.main"
                                       :infer-externs true
                                       :output-dir    "target/expo"
                                       :optimizations :none}}

                       {:id           "release"
                        :source-paths ["src"]
                        :compiler     {:output-to          "main.js"
                                       :main               "env.expo.main"
                                       :static-fns         true
                                       :fn-invoke-direct   true
                                       :externs            ["js/externs.js"]
                                       :infer-externs      true
                                       :parallel-build     true
                                       :optimize-constants true
                                       :optimizations      :advanced
                                       :closure-defines    {"goog.DEBUG" false}}}]})
