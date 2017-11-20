(ns hive.effects)

(defn navigate!
  [stack {:keys [navigation]} destination]
  (let [{:keys [navigate]} navigation
        nstack (conj stack destination)]
    (navigate destination)
    [{:route/stack nstack}]))

(defn go-back!
  [stack {:keys [navigation]}]
  (let [{:keys [goBack]} navigation
        nstack (pop stack)]
    (goBack)
    [{:route/stack nstack}]))

