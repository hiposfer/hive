(ns hive.utils.promises
  "utility functions to get better integration of promises with transact!")

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

(defn bypass
  "same as guard but does nothing on error"
  [effect error-id]
  (let [[f & args] effect]
    (.. (apply f args)
        (catch (constantly nil)))))

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

