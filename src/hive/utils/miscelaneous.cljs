(ns hive.utils.miscelaneous
  "a namespace for functions that have not found a home :'("
  (:require [clojure.spec.alpha :as s]
            [js-quantities :as quantity]
            [cljs.core.async.impl.protocols :as asyncpro]))

;; HACK: https://stackoverflow.com/questions/27746304/how-do-i-tell-if-an-object-is-a-promise
(defn promise?
  [value]
  (and (some? value) (exists? (.-then value))))

(defn with-ns
  "modify a map keys to be namespaced with ns"
  [ns m]
  (zipmap (map #(keyword ns %) (keys m))
          (vals m)))

(defn keywordize
  "transforms a js object into a clojure version with keywords as keys"
  [o]
  (js->clj o :keywordize-keys true))

(defn error?
  "checks if o is an instance of the Javascript base type Error"
  [o] (instance? js/Error o))

(defn reject-on-error
  "reject a promise if its value is an error"
  [v]
  (if (error? v) (throw v) v))

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

(defn convert
  "converts value from a measurement unit to another. Returns
  the scalar value in the final unit scale."
  [value & {:keys [from to precision]}]
  (.. (quantity value from)
      (to to)
      (toPrec (or precision 0.1))
      -scalar))

(defn roundtrip
  "HACK: takes an instance of a Js Class, stringifies through JSON and reads
  it back as a clojure map.

  Useful for cases where js->clj does't work due to the instance not being
  a native js object"
  [object]
  (let [text (js/JSON.stringify object)
        parsed (js/JSON.parse text)]
    (js->clj parsed :keywordize-keys true)))

(def nullify (constantly nil))

(defn channel?
  [x]
  (satisfies? asyncpro/Channel x))

(defn hour-minute
  "returns the epoch-seconds time as HH:MM in the local time"
  [epoch-seconds]
  (let [text (.toLocaleTimeString (new js/Date (* 1000 epoch-seconds))
                                  "de-De")]
    (subs text 0 5)))

(def default-color "#3bb2d0")

(defn route-color
  [route]
  (or (:route/color route)
      (when (some? route)
        (color (str (or (:route/long_name route)
                        (:route/short_name route)))))
      default-color))

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
