(defproject hive "0.1.0-SNAPSHOT"
  :description "Your go-to routing app for public transport"
  :url "https://github.com/hiposfer/hive"
  :license {:name "LGPL v3"
            :url  "https://choosealicense.com/licenses/gpl-3.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [react-native-externs "0.2.0"]
                 [org.clojure/core.async "0.3.465"]
                 [reagent "0.7.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server cljsjs/create-react-class]]
                 [datascript "0.16.2"]
                 [binaryage/oops "0.6.1"]
                 [cljs-react-navigation "0.1.1"]
                 [hiposfer/geojson.specs "0.2.0"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.14"]]
  :clean-targets ["target/" "main.js"]
  :aliases {"figwheel"        ["run" "-m" "user" "--figwheel"]
            "rebuild-modules" ["run" "-m" "user" "--rebuild-modules"]
            "prod-build"      ["do" ["clean"]
                                    ["with-profile" "prod" "cljsbuild" "once" "main"]]}
  :profiles {:dev  {:dependencies [[figwheel-sidecar "0.5.14"]
                                   [com.cemerick/piggieback "0.2.1"]
                                   [expound "0.7.0"]
                                   [org.clojure/test.check "0.9.0"]]
                    :source-paths ["src" "env/dev"]
                    :cljsbuild    {:builds [{:id           "main"
                                             :source-paths ["src" "env/dev"]
                                             :figwheel     true
                                             :compiler     {:output-to     "target/expo/not-used.js"
                                                            :main          "env.expo.main"
                                                            :infer-externs  true
                                                            :output-dir    "target/expo"
                                                            :optimizations :none}}]}
                    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :prod {:cljsbuild {:builds [{:id           "main"
                                          :source-paths ["src" "env/prod"]
                                          :compiler     {:output-to          "main.js"
                                                         :main               "env.expo.main"
                                                         :output-dir         "target/expo"
                                                         :static-fns         true
                                                         :externs            ["js/externs.js"]
                                                         :infer-externs      true
                                                         :parallel-build     true
                                                         :optimize-constants true
                                                         :optimizations      :advanced
                                                         :closure-defines    {"goog.DEBUG" false}}}]}}})
