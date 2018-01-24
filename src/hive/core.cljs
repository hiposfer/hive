(ns hive.core
  (:require [reagent.core :as r]
            [hive.foreigns :as fl]
            [hive.components.core :refer [View Image Text TouchableHighlight Icon]]
            [hive.state :as state]
            [hive.services.location :as position]
            [hive.services.raw.location :as location]
            [hive.components.navigation :as nav]
            [hive.rework.core :as work :refer-macros [go-try <?]]
            [datascript.core :as data]
            [hive.services.store :as store]
            [hive.queries :as queries]
            [hive.rework.util :as tool]
            [hive.components.screens.home :as home]
            [hive.components.screens.settings :as settings]))

(defn root-ui []
  (let [Root     (nav/drawer-navigator {:Home {:screen home/Screen}
                                        :Settings {:screen settings/Screen}}
                                       {})]
    [:> Root]))

(defn register! []
  (.registerComponent fl/app-registry "main"
                      #(r/reactify-component root-ui)))

(def reload-config!
  (work/pipe store/load!
             (tool/validate not-empty ::missing-data)
             (work/inject :user/id queries/user-id)
             vector
             work/transact!))

(defn init!
  "register the main UI component in React Native"
  [] ;; todo: add https://github.com/reagent-project/historian
  (let [conn   (data/create-conn state/schema)
        data   (cons {:app/session (data/squuid)}
                     state/init-data)]
    (data/transact! conn data)
    (work/init! conn)
    (go-try (work/transact! (<? (location/watch! position/defaults)))
            (catch :default error (fl/toast! (ex-message error))))
    (go-try (<? (reload-config! [:user/city]))
            (catch :default error
              (let [base [(work/inject state/defaults :user/id queries/user-id)]]
                (work/transact! base)
                (tool/log! error)))
            (finally (register!)))))

;(async/take! (store/delete! [:user/city]) println)

;; this also works but it is not as clear
;(async/take! (location/watch! {::location/enableHighAccuracy true
;                               ::location/timeInterval 3000})
;             cljs.pprint/pprint)

;(:eavt @hive.rework.state/conn)


;; FOOD FOR THOUGHT
;; a possible way of synchronizing the entire datascript option would
;; be to create a serializer which takes the datascript content and stores
;; it using the db/id (Values with the same id are merged together).
;; This however would be very inneficient, therefore it would be best
;; to throtle it to say every 15 seconds or so.
;; Furthermore, storing the complete state again would be very inneficient
;; so a complete diff of the two states should be performed and only those
;; that changed should be stored.
;; Did I forget anything there?
;; PS: simply use add-watch to the datascript conn
