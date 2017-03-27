(ns hive.subs
  (:require [re-frame.subs :as subs]))

(defn map-center [db event] (:map/center db))
(defn map-zoom [db event] (:map/zoom db))
(defn map-annotations [db event] (:map/annotations db))
