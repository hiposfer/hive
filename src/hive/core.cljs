(ns hive.core
  (:require [reagent.core :as r]
            [expo :as Expo]
            [react-native :as React]
            [hive.state.core :as state]
            [hive.services.firebase :as firebase]
            [datascript.core :as data]
            [hive.services.sqlite :as sqlite]
            [hive.state.queries :as queries]
            [hive.utils.miscelaneous :as misc]
            [hive.utils.promises :as promise]
            [hive.screens.home.core :as home]
            [hive.screens.home.gtfs :as gtfs]
            [hive.screens.errors :as errors]
            [hive.screens.router :as router]
            [hive.screens.settings.core :as settings]
            [hive.screens.settings.city-picker :as city-picker]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.screens.home.directions :as route]
            [hive.services.kamal :as kamal]))

(defn- MessageTray
  [props]
  (let [id      @(state/q! queries/session)
        alert   @(state/pull! [:session/alert]
                             [:session/uuid id])]
    (when-not (empty? (:session/alert alert))
      [:> React/View {:flex 1 :justifyContent "flex-end" :alignItems "center"
                      :bottom 0 :width "100%" :height "5%" :position "absolute"}
        [:> React/Text
          {:style {:width "100%" :height "100%" :textAlign "center"
                   :backgroundColor "grey" :color "white"}
           :onPress #(state/transact! [{:session/uuid id :session/alert {}}])}
          (:session/alert alert)]])))

(defn- screenify
  [component]
  (rn-nav/stack-screen
    (fn [props]
      [:> React/View {:flex 1 :alignItems "stretch"}
        [component props]
        [MessageTray props]])))

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
                    {:home           {:screen (screenify home/Home)}
                     :directions     {:screen (screenify route/Instructions)}
                     :gtfs           {:screen (screenify gtfs/Data)}
                     :settings       {:screen (screenify settings/Settings)}
                     :select-city    {:screen (screenify city-picker/Selector)}}
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
  [connected]
  (let [sid @(state/q! queries/session)]
    (if-not connected
      (state/transact! [{:session/uuid sid :session/alert "You are offline."}]))))

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
  (state/transact! [{:session/uuid (data/squuid)
                     :session/start (js/Date.now)}])
  ;; firebase related functionality ...............
  (firebase/init!)
  ;; restore user data ...........................
  (.. (sqlite/init!)
      (then state/transact!)
      ;; listen only AFTER restoration
      (then #(sqlite/listen! state/conn))
      ;; create a user/id placeholder if it doesnt exists
      (then #(state/transact! [{:user/uid (or (data/q queries/user-id (state/db)) "")}]))
      ;; execute only after we are sure that we have a user id
      (then #(state/transact! [[promise/finally [kamal/get-areas!]
                                                [init-areas (state/db)]]
                               ;; todo: I guess firebase should handle this?
                               [firebase/sign-in! (state/db)]])))
  ;; start listening for events ..................
  (Expo/registerRootComponent (r/reactify-component RootUi))
  ;; handles Android BackButton
  (React/BackHandler.addEventListener "hardwareBackPress"
                                      back-listener!)
  (React/NetInfo.isConnected.addEventListener "connectionChange"
                                              internet-connection-listener))

;(. (sqlite/init!) (then cljs.pprint/pprint))
;(. (sqlite/clear!) (then cljs.pprint/pprint))