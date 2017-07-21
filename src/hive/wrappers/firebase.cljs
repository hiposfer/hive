(ns hive.wrappers.firebase
  (:require [hive.foreigns :as fl]))

(defn init! [token] (.initializeApp fl/FireBase (clj->js token)))

(defn sign-in-anonymously!
  "Original docs: https://firebase.google.com/docs/auth/web/anonymous-auth
  Returns Firebase.User type: https://firebase.google.com/docs/reference/js/firebase.User"
  [[{:keys [on-success on-error] :or {on-success identity, on-error identity}}]]
  (-> fl/FireBase
      (.auth)
      (.signInAnonymously)
      (.then on-success)
      (.catch on-error)))
  ;(sign-in-anonymously! [(fn [user] (let [cuser (js->clj user :keywordize-keys true)]
  ;                                    (println "USER: " (js-keys cuser))
  ;                                    (println (type cuser))))
  ;                       #(println "ERROR: " %)])

(defn set-value!
  "set id to value in Firebase"
  [[id value]]
  (.set (.ref (.database fl/FireBase) id)
        (clj->js value)))

(defn report!
  [[id value]]
  (case id
    :geocode/miss (set-value! [(str "geocode-errors/" (js/Date.))
                               value])))