(ns hive.subs
  (:require [re-frame.subs :as subs]))

(defn map-center [db event] (:map/center db))
(defn map-zoom [db event] (:map/zoom db))

(defn user-location [db event] (:user/location db))
(defn user-targets [db event] (:user/targets db))
(defn view-targets? [db event] (:view/targets db))
