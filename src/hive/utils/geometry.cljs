(ns hive.utils.geometry)

;; Note in these scripts, I generally use
;; - latitude, longitude in degrees
;; - φ for latitude  in radians
;; - λ for longitude in radians
;; having found that mixing degrees & radians is often the easiest route to
;; head-scratching bugs...

(def RADIOUS 6372800); radious of the Earth in meters

(defn radians
  [angle]
  (* angle (/ Math/PI 180)))

(defn haversine
  "Compute the great-circle distance between two points on Earth given their
  longitude and latitude in DEGREES. The distance is computed in meters"
  ([[lon-1 lat-1] [lon-2 lat-2]]
   (let [φ1 (radians lat-1)
         φ2 (radians lat-2)
         Δφ (radians (- lat-2 lat-1))
         Δλ (radians (- lon-2 lon-1))
         a  (+ (Math/pow (Math/sin (/ Δφ 2)) 2)
               (* (Math/pow (Math/sin (/ Δλ 2)) 2)
                  (Math/cos φ2)
                  (Math/cos φ1)))]
     (* RADIOUS 2 (Math/asin (Math/sqrt a))))))

(defn latlng
  [coordinates]
  {:latitude (second coordinates) :longitude (first coordinates)})
