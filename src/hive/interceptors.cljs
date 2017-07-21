(ns hive.interceptors
  "see https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md"
  (:require [cljs.spec.alpha :as s]
            [re-frame.std-interceptors :as nsa]
            [re-frame.interceptor :as fbi]
            [expound.alpha :as expound]))

;; -- Interceptors ------------------------------------------------------------
;;
;; https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md
;;

(defn before
  "wrapper for creating 'before' interceptors"
  ([f] (fbi/->interceptor :id :before :before f))
  ([id f] (fbi/->interceptor :id id :before f)))

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [_]]
  (when-not (s/valid? spec db)
    (throw (expound/expound-str spec db))))

(def validate "interceptor to check valid db state"
  (if-not goog.DEBUG []
    (nsa/after (partial check-and-throw :hive/state))))