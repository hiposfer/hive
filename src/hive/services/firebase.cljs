(ns hive.services.firebase
  (:require [hive.rework.util :as tool]
            [datascript.core :as data]))

(def ref ^js/Firebase (js/require "firebase"))

(defn auth-listener
  "takes the database and a UserCredential from firebase and returns a
  transaction to change the user information

  https://firebase.google.com/docs/reference/js/firebase.auth#.UserCredential"
  [db result]
  (when (some? result) ;; todo: handle user sign out properly
    (let [e    (data/q '[:find ?e . :where [?e :user/uid]] db)
          ;; workaround based on https://stackoverflow.com/a/51439387
          data (js->clj (.. result -user (toJSON)) :keywordize-keys true)]
      [(merge (tool/with-ns "user" (into {} (for [[k v] data :when (some? v)] [k v])))
              {:db/id e})])))

(defn sign-up!
  [db]
  (let [[email password] (data/q '[:find [?email ?password]
                                   :where [?user :user/uid]
                                          [?user :user/email ?email]
                                          [?user :user/password ?password]]
                                 db)
        credential (.. ref (auth.EmailAuthProvider.credential email password))]
    (.. ref (auth) -currentUser
        (linkAndRetrieveDataWithCredential credential)
        (then #(do (.. % -user (sendEmailVerification))
                   (auth-listener db %)))
        (catch js/console.error))))

(defn sign-in!
  [db]
  (let [[email password] (data/q '[:find [?email ?password]
                                   :where [?user :user/uid]
                                          [?user :user/email ?email]
                                          [?user :user/password ?password]]
                                 db)]
    (if (and (some? email) (some? password))
      (.. ref
          (auth)
          (signInWithEmailAndPassword email password)
          (then #(auth-listener db %)))
      ;; we dont have a user registered - sign in anonymously
      (.. ref
          (auth)
          (signInAnonymously)
          (then #(auth-listener db %))
          (catch js/console.error)))))
