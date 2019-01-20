(ns hive.utils.miscelaneous
  "a namespace for functions that have not found a home :'("
  (:require [clojure.spec.alpha :as s]
            [js-quantities :as quantity]
            [cljs.reader :as edn]))

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

(defn validate
  "validates the request against the provided spec. Returns the request if valid
  or an ex-info with cause otherwise"
  ([spec value cause]
   (if (s/valid? spec value) value
     (ex-info (s/explain-str spec value)
              (assoc (s/explain-data spec value) ::reason cause))))
  ([spec cause]
   (fn validate* [value] (validate spec value cause)))
  ([spec]
   (fn validate* [value] (validate spec value ::invalid-data))))

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
  "takes an custom Js Object, stringifies through JSON and reads
  it back as a clojure map.

  Useful for cases where js->clj doesnt work"
  [object]
  (let [text (js/JSON.stringify object)
        parsed (js/JSON.parse text)]
    (js->clj parsed :keywordize-keys true)))

(defn- on-fetch-response
  [^js/Response response]
  (if (.-ok response)
    (. response (text))
    (.. (. response (text))
        (then #(throw (ex-info (str "Error fetching directions." %)
                               (roundtrip response)))))))

(def nullify (constantly nil))