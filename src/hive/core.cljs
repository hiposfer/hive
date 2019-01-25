(ns hive.core
  (:require [reagent.core :as r]
            [expo :as Expo]
            [cljs.core.async :as async]
            [react-native :as React]
            [hive.state.core :as state]
            [datascript.core :as data]
            [hive.services.sqlite :as sqlite]
            [hive.state.queries :as queries]
            [hive.utils.miscelaneous :as misc]
            [hive.utils.promises :as promise]
            [hive.screens.home.core :as home]
            [hive.screens.router :as router]
            [hive.screens.settings.core :as settings]
            [hive.screens.settings.city-picker :as city-picker]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.screens.home.directions.core :as directions]
            [hive.services.kamal :as kamal]))

(defn RootUi []
  "Each Screen will receive two props:
   - screenProps - Extra props passed down from the router (rarely used)
   - navigation  - The main navigation functions in a map as follows:
     {:state     - routing state for this screen
      :dispatch  - generic dispatch fn
      :goBack    - pop's the current screen off the stack
      :navigate  - most common way to navigate to the next screen
      :setParams - used to change the params for the current screen}"
  (let [Navigator (rn-nav/stack-navigator
                    {:home        {:screen (rn-nav/stack-screen home/Screen)}
                     :directions  {:screen (rn-nav/stack-screen directions/Screen)}
                     :settings    {:screen (rn-nav/stack-screen settings/Screen)}
                     :select-city {:screen (rn-nav/stack-screen city-picker/Screen)}}
                    {:headerMode "none"})]
    [router/Router {:root Navigator :init "home"}]))

(defn- back-listener!
  "a generic Android back button listener which pops the last element from the
  navigation stack or exists otherwise.

  Note: for Component specific back listeners it might be necessary to unsubscribe
  this listener and subscribe your own for the lifecycle of the UI component.
  See `with-let` https://reagent-project.github.io/news/news060-alpha.html"
  []
  (let [result      (data/q router/data-query (state/db))
        transaction (router/goBack result)]
    (cond
      ;; no router initialized, Exit
      (nil? (second result))
      false

      ;; nothing to go back to, Exit
      (= (misc/keywordize (first result))
         (misc/keywordize (:react.navigation/state (first transaction))))
      false

      ;; always returns true to prevent native side handling of back button
      :else (some? (state/transact! transaction)))))

(defn- internet-connection-listener
  "Listens to connection changes mainly for internet access."
  [ConnectionType db]
  (let [session         (data/q queries/session db)
        connection-type (js->clj ConnectionType
                                 :keywordize-keys true)]
    [(merge {:session/uuid session}
            (misc/with-ns "connection" connection-type))]))

(defn- init-areas
  "When we start hive for the first time, we dont have any data on the supported
  areas. We need to download it and link it to the user"
  [areas-or-error db]
  (let [areas (data/q queries/kamal-areas db)
        user  (data/q queries/user-entity db)]
    (cond
      ;; we dont have any data we failed to fetch it
      (and (misc/error? areas-or-error) (empty? areas))
      [{:error/id :init/failed :error/info areas-or-error}]

      ;; for simplicity, choose the first area
      (and (not (misc/error? areas-or-error)) (empty? areas))
      (concat areas-or-error
              [{:db/id user
                :user/area [:area/id (:area/id (first areas-or-error))]}])

      ;; we received data from the server, simply overwrite the current one
      (not (misc/error? areas-or-error))
      areas-or-error)))

(defn init!
  "register the main UI component in React Native"
  []
  ;; start listening for events ..................
  (Expo/registerRootComponent (r/reactify-component RootUi))
  ;; handles Android BackButton
  (React/BackHandler.addEventListener "hardwareBackPress" back-listener!)
  (React/NetInfo.addEventListener "connectionChange"
    #(state/transact! (internet-connection-listener % (state/db))))

  (async/go
    (state/transact! [{:session/uuid (data/squuid)
                       :session/start (js/Date.now)}
                      {:user/uid ""}])
    ;; firebase related functionality ...............
    ;; (firebase/init!)
    ;; restore user data ...........................
    (let [previous-data (promise/async (sqlite/init!))
          internet?     (promise/async (React/NetInfo.getConnectionInfo))
          areas         (kamal/get-areas!)]
      (state/transact! (async/<! previous-data))
      ;; listen only AFTER restoration
      (sqlite/listen! state/conn)
      ;; execute only after we are sure that we have a user id
      (state/transact! (init-areas (async/<! areas) (state/db)))
      (state/transact! (internet-connection-listener (async/<! internet?)
                                                     (state/db))))))
;(. (sqlite/init!) (then cljs.pprint/pprint))
;(. (sqlite/clear!) (then cljs.pprint/pprint))
