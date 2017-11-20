(ns hive.effects
  (:require [hive.rework.core :as rework]
            [hive.queries :as queries]))

;(defn ->screen
;  [stack {:keys [navigation]} destination]
;  (let [{:keys [navigate]} navigation
;        nstack (conj stack destination)]
;    (navigate destination)
;    [{:route/stack nstack}]))
;
;(defn go-back
;  [stack {:keys [navigation]}]
;  (let [{:keys [goBack]} navigation
;        nstack (pop stack)]
;    (goBack)
;    [{:route/stack nstack}]))

(defn move-to
  [user-id city-name]
  [{:user/id user-id
    :user/city [:city/name city-name]}])