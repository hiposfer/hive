(ns hive.events
  (:require [clojure.spec :as s]
            [hive.core :as hive]
            [re-frame.std-interceptors :as nsa]
            [re-frame.interceptor :as fbi]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [hive.secrets :as secrets]
            [hive.effects :as effects]
            [re-frame.router :as router]
            [hive.foreigns :as fl]))

;; -- Interceptors ------------------------------------------------------------
;;
;; https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check after " event " failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (nsa/after (partial check-and-throw ::hive/state))
    []))

(defn before
  "wrapper for creating 'before' interceptors"
  ([f] (fbi/->interceptor :id :before :before f))
  ([id f] (fbi/->interceptor :id id :before f)))

(defn map-token
  [context] ;; extract db and event from coeffects
  (let [[id name handler] (:event (:coeffects context))]
    (assoc-in context [:coeffects :event]
      [id "mapbox.places" name {:access_token (:mapbox secrets/tokens)} handler])))

(defn carmen->targets
  "takes a json object returned by mapbox carmen-geojson style and modifies
   the coeffects map to contain an event with mapbox-compatible annotations"
  [context]
  (let [[id carmen-geojson] (:event (:coeffects context))
        native     (js->clj carmen-geojson :keywordize-keys true)
        interest   (filter (comp #{"Point"} :type :geometry) (:features native))
        points     (map (comp reverse :coordinates :geometry) interest)
        names      (map (comp first #(str/split % #",") :place_name) interest)
        addresses  (map (comp :address :properties) interest)
        markers    (sequence (map effects/mark) points names addresses)]
    ;(cljs.pprint/pprint interest)
    (assoc-in context [:coeffects :event] [id markers])))

(defn bias-geocode
  [context] ;; extract db and event from coeffects
  (let [[id kind name params handler] (:event (:coeffects context))
        position  (:user/location (:db context))
        prox      (some->> position #(vector (:longitude %) (:latitude %))
                                     (str/join ","))
        bounds    (str/join "," (:bbox (:user/city (:db (:coeffects context)))))]
    (if (nil? position)
      (assoc-in context [:coeffects :event]
                [id kind name (merge params {:bbox bounds}) handler])
      (assoc-in context [:coeffects :event]
        [id kind name (merge params {:proximity prox :bbox bounds} handler)]))))

;; -- Handlers --------------------------------------------------------------

(defn init [_ _] hive/state)

(defn geocode
  [cofx [id kind query url-param handler]]
  (let [template "https://api.mapbox.com/geocoding/v5/{mode}/{query}.json?{params}"
        params   (reduce-kv (fn [res k v] (conj res (str (name k) "=" (js/encodeURIComponent v))))
                            [] url-param)
        URL (-> (str/replace template "{mode}" kind)
                (str/replace "{query}" (js/encodeURIComponent query))
                (str/replace "{params}" (str/join "&" params)))]
    {:fetch/json [URL {} handler]}))

;; These handlers take two arguments;  `coeffects` and `event`, and they return `effects`.
(defn navigate-back
  [cofx [id]]
  (println "view: " (:view/screen (:db cofx)))
  (condp = (:view/screen (:db cofx))
    :home (if (:view/targets (:db cofx))
            {:db (assoc (:db cofx) :view/targets false)}
            {:app/exit true})
    {:app/exit true}));(do (.exitApp fl/back-android) {})))