(ns hive.interceptors
  "see https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md"
  (:require [cljs.spec :as s]
            [re-frame.std-interceptors :as nsa]
            [re-frame.interceptor :as fbi]
            [clojure.string :as str]
            [hive.util :as util]
            [hive.geojson :as geojson]))

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
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "INVALID STATE: " (s/explain-str spec db)) explain-data)))))

(def validate "interceptor to check valid db state"
  (if-not goog.DEBUG []
    (nsa/after (partial check-and-throw :hive/state))))

(defn bypass-geocode
  "takes the parameters passed to create a MapBox geocode call and inserts the api
   token into the events parameters to avoid overly long events calls"
  [context] ;; extract db and event from coeffects
  (let [[id name handler] (:event (:coeffects context))
        token             (:mapbox (:tokens (:db (:coeffects context))))]
    (assoc-in context [:coeffects :event]
              [id "mapbox.places" name {:access_token token} handler])))

(defn bypass-directions
  "takes the parameters passed to create a MapBox directions call and inserts the api
   token into the events parameters to avoid overly long events calls"
  [context] ;; extract db and event from coeffects
  (let [[id coordinates handler] (:event (:coeffects context))
        token                    (:mapbox (:tokens (:db (:coeffects context))))]
    (assoc-in context [:coeffects :event]
              [id "mapbox/driving" coordinates ;;TODO: steps -> turn by turn navigation
               {:access_token token :geometries "geojson"}; :steps true}
               handler])))

(defn bias-geocode
  "takes the parameters used to make a geocode call and insert the position
  to bias the results towards it. It also inserts a bounding box based on
  the user's current area/city)"
  [context] ;; extract db and event from coeffects
  (let [[id kind name params handler] (:event (:coeffects context))
        position  (:user/location (:db context))
        prox      (some->> position geojson/uri);FIXME city make geojson compatible
        bounds    (str/join "," (geojson/bbox (:user/city (:db (:coeffects context)))))]
    (if (nil? position)
      (assoc-in context [:coeffects :event]
                [id kind name (merge params {:bbox bounds}) handler])
      (assoc-in context [:coeffects :event]
                [id kind name (merge params {:proximity prox :bbox bounds} handler)]))))
