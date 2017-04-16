(ns hive.subs
  (:require [re-frame.subs :as subs]))

(defn user-location [db event] (:user/location db))
(defn user-city [db event] (:user/city db))
(defn user-targets [db event] (:user/targets db))

(defn view-targets? [db event] (:view/targets db))
(defn view-screen [db event] (:view/screen db))

