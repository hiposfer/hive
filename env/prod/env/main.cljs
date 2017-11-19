(ns env.main
  (:require [hive.core :as core]))

;; init
(reset! app (core/system))
;; start
(reset! app (component/start @app))
