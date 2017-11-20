(ns env.main
  (:require [hive.core :as core]
            [hive.rework.core :as rework]))

(rework/init! (component/start (core/system)))