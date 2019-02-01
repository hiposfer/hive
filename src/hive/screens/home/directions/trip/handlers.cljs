(ns hive.screens.home.directions.trip.handlers
  (:require [hive.schema :as schema]
            [hiposfer.gtfs.edn :as gtfs]
            [clojure.string :as str]))

;; adapted from
;; https://stackoverflow.com/a/16348977
(defn color
  [text]
  (let [h (reduce (fn [res i] (+ (. text (charCodeAt i))
                                 (- (bit-shift-left res 5)
                                    res)))
                  0
                  (range (count text)))]
    (reduce (fn [res i]
              (let [v (bit-and (bit-shift-right h (* i 8)) 0xFF)]
                (str res (. (str "00" (. v (toString 16)))
                            (substr -2)))))
            "#"
            (range 3))))

(def default-color "#3bb2d0")

(defn route-color
  [route]
  (or (:route/color route)
      (when (some? route)
        (color (str (or (:route/long_name route)
                        (:route/short_name route)))))
      default-color))

(defn hour-minute
  "returns the epoch-seconds time as HH:MM in the local time"
  [epoch-seconds]
  (let [text (.toLocaleTimeString (new js/Date (* 1000 epoch-seconds))
                                  "de-De")]
    (subs text 0 5)))

(defn seconds-of-day
  [^js/Date date]
  (+ (. date (getSeconds))
     (* 60 (+ (. date (getMinutes))
              (* 60 (. date (getHours)))))))

(defn time-since-midnight
  [seconds]
  (let [now (new js/Date)]
    (doto now (.setHours 0 0 0 0)
              (.setSeconds seconds))
    (hour-minute (/ (. now (getTime)) 1000))))

(defn datoms->map
  [datoms replacements]
  (into {} (for [[_ a v] datoms]
             (if (contains? replacements a)
               [a (get replacements a)]
               [a v]))))

(defn route-type-name
  [trip]
  (let [vehicles    (for [entry (:values (gtfs/get-mapping schema/gtfs-data
                                                           :route/type))
                          :when (= (:value entry)
                                   (:route/type (:trip/route trip)))]
                      (first (str/split (:description entry) #"\.|,")))]
    (str (first vehicles) " "
         (:route/short_name (:trip/route trip)))))
