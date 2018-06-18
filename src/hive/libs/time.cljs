(ns hive.libs.time)


(defn- pad
  [number]
  (if (< number 10)
    (str "0" number)
    number))

(defn date-time
  "returns a local date time string representation as per Java time LocalDateTime parse"
  ([]
   (date-time (new js/Date)))
  ([^js/Date value]
   (str (-> value (.getFullYear))
        "-"
        (pad (inc (-> value (.getMonth))))
        "-"
        (pad (-> value (.getDate)))
        "T"
        (pad (-> value (.getHours)))
        ":"
        (pad (-> value (.getMinutes)))
        ":"
        (pad (-> value (.getSeconds))))))

