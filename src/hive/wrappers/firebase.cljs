(ns hive.wrappers.firebase)

(defonce FireBase (js/require "firebase"))

(defn init! [token](.initializeApp FireBase token))

(defn sign-in-anonymously!
  "Original docs: https://firebase.google.com/docs/auth/web/anonymous-auth
  Returns Firebase.User type: https://firebase.google.com/docs/reference/js/firebase.User"
  [[on-success on-error]]
  (-> (FireBase.auth.signInAnonymously)
      (.then on-success)
      (.catch on-error)))
  ;(sign-in-anonymously! [(fn [user] (let [cuser (js->clj user :keywordize-keys true)]
  ;                                    (println "USER: " (js-keys cuser))
  ;                                    (println (type cuser))))
  ;                       #(println "ERROR: " %)])