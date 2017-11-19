(ns env.main
  (:require [hive.core :as core]))

;; init
(reset! core/app (core/system))
;; start
(reset! core/app (component/start @core/app))
