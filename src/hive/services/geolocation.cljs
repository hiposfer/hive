(ns hive.services.geolocation)

(defn update!
  [data]
  [{:user/id (:user/id data)
    :user/position (dissoc data :user/id)}])
