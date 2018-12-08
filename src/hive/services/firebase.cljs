(ns hive.services.firebase
  (:require [hive.utils.miscelaneous :as tool]
            [datascript.core :as data]
            [hive.state.queries :as queries]
            [hive.state.core :as state]))

;; it seems that firebase creates an instance of itself when requiring it so
;; using the ns form doesnt bring any benefits :(
(def ref ^js/Firebase (js/require "firebase"))

(defn init!
  []
  (let [config #js {:apiKey        (:ENV/FIREBASE_API_KEY state/tokens)
                    :authDomain    (:ENV/FIREBASE_AUTH_DOMAIN state/tokens)
                    :databaseUrl   (:ENV/FIREBASE_DATABASE_URL state/tokens)
                    :storageBucket (:ENV/FIREBASE_STORAGE_BUCKET state/tokens)}]
    (. ref (initializeApp config))))

(defn update-user-info
  "takes the database and a UserCredential from firebase and returns a
  transaction to change the user information

  https://firebase.google.com/docs/reference/js/firebase.auth#.UserCredential"
  [db result]
  (when (some? result)
    ;; todo: handle user sign out properly
    (let [e      (data/q '[:find ?e . :where [?e :user/uid]] db)
          errors (data/q '[:find [?e ...] :where [?e :auth/error]] db)
          ;; workaround based on https://stackoverflow.com/a/51439387
          raw    (tool/keywordize (.. result -user (toJSON)))
          user   (tool/with-ns "user" (into {} (filter #(some? (val %))) raw))]
      ;; remove useless nil values
      (cons (merge user {:db/id e})
            (for [e errors] [:db/retractEntity e])))))

(defn sign-up!
  [db]
  (let [[email password] (data/q queries/email&password db)
        credential (.. ref (auth.EmailAuthProvider.credential email password))]
    (.. ref (auth) -currentUser
        (linkAndRetrieveDataWithCredential credential)
        (then #(do (.. % -user (sendEmailVerification))
                   (update-user-info db %))))))

(defn sign-in!
  [db]
  (let [[email password] (data/q queries/email&password db)]
    (if (and (some? email) (some? password))
      (.. ref
          (auth)
          (signInWithEmailAndPassword email password)
          (then #(update-user-info db %)))
      ;; we dont have a user registered - sign in anonymously
      (.. ref
          (auth)
          (signInAnonymously)
          (then #(update-user-info db %))))))

(defn sign-in-or-up!
  "tries to create a user with the email/password combination in the db,
   or signs in otherwise"
  [db]
  (.. (sign-in! db)
      (catch (fn [^js/AuthError error]
               (if (= "auth/user-not-found" (. error -code))
                 [{:auth/error (tool/keywordize (. error (toJSON)))}]
                 (sign-up! db))))))

;hive.rework.state/conn
;(sign-in-or-up! (state/db))
