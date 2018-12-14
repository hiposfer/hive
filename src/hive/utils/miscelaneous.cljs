(ns hive.utils.miscelaneous
  "a namespace for functions that have not found a home :'("
  (:require #_[cljs.core.async :as async]
            [clojure.spec.alpha :as s]))

#_(defn chan? [x] (satisfies? cljs.core.async.impl.protocols/Channel x))

#_(defn async
    "transforms a promise into a channel. Catches js/Errors and puts them in the
  channel as well. If the catch value is not an error, yields an ex-info with
  ::oops as message. Accepts a transducer that applies to the channel"
    [promise & xforms]
    (let [result (if (empty? xforms)
                   (async/promise-chan)
                   (async/promise-chan (apply comp xforms)))]
      (-> promise
          (.then #(do (async/put! result %)
                      (async/close! result)))
          (.catch #(if (instance? js/Error %)
                     (async/put! result %)
                     (async/put! result (ex-info ::oops %)))))
      result))

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

(defn guard
  "guards the execution of an effect promise with a catch statement that will
  return a transaction on [{:error/id error-id}] with the information from the
  error.

  Promotes all errors to Clojure's ex-info"
  [effect error-id]
  (let [[f & args] effect]
    (.. (apply f args)
        (catch
          (fn [error]
            (if (instance? ExceptionInfo error)
              [{:error/id error-id :error/info error}]
              [{:error/id error-id :error/info (ex-info (ex-message error)
                                                        error)}]))))))

(defn chain
  "chains the execution of f to the result of effect; prepending it
  to the arguments"
  [effect [f & args]]
  (let [[fe & fe-args] effect]
    (.. (apply fe fe-args)
        (then (fn [result] (apply f (cons result args)))))))

(defn finally
  "regardless of the success or result of effect, passes it to f;
   prepending it to the arguments"
  [effect [f & args]]
  (let [[fe & fe-args] effect]
    (.. (apply fe fe-args)
        (then (fn [result] (apply f (cons result args))))
        (catch (fn [error] (apply f (cons error args)))))))
