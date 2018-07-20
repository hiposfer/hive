(ns hive.core
  (:require [reagent.core :as r]
            [hive.foreigns :as fl]
            [hive.state :as state]
            [hive.rework.core :as work]
            [hive.components.screens.home.welcome :as welcome]
            [datascript.core :as data]
            [hive.services.store :as store]
            [hive.queries :as queries]
            [hive.rework.util :as tool]
            [hive.components.screens.home.core :as home]
            [hive.components.screens.errors :as errors]
            [hive.components.router :as router]
            [hive.components.screens.settings.core :as settings]
            [hive.components.screens.settings.city-picker :as city-picker]
            [cljs.core.async :as async]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.components.screens.home.route :as route]
            [hive.components.foreigns.react :as react]))

(defn- MessageTray
  [props]
  (let [id      (data/q queries/session (work/db))
        alert   @(work/pull! [:session/alert]
                             [:session/uuid id])
        height (if (empty? (:session/alert alert)) 0 "5%")]
    [:> react/View {:flex 1 :justifyContent "flex-end" :alignItems "center"
                    :bottom 0 :width "100%"
                    :height height :position "absolute"}
      [:> react/Text
        {:style {:width "100%" :height "100%" :textAlign "center"
                 :backgroundColor "grey" :color "white"}
         :onPress #(work/transact! [{:session/uuid id :session/alert {}}])}
        (:session/alert alert)]]))

(defn- screenify
  [component props]
  (rn-nav/stack-screen
    (fn [props]
      [:> react/View {:flex 1 :alignItems "stretch"}
        [component props]
        [MessageTray props]])
    props))

(defn RootUi []
  "Each Screen will receive two props:
   - screenProps - Extra props passed down from the router (rarely used)
   - navigation  - The main navigation functions in a map as follows:
     {:state     - routing state for this screen
      :dispatch  - generic dispatch fn
      :goBack    - pop's the current screen off the stack
      :navigate  - most common way to navigate to the next screen
      :setParams - used to change the params for the current screen}"
  (let [Navigator
    (rn-nav/stack-navigator
      {:home           {:screen (screenify home/Home {:title "map"})}
       :welcome        {:screen (screenify welcome/Login {:title "welcome"})}
       :directions     {:screen (screenify route/Instructions {:title "directions"})}
       :settings       {:screen (screenify settings/Settings {:title "settings"})}
       :select-city    {:screen (screenify city-picker/Selector {:title "Select City"})}
       :location-error {:screen (screenify errors/UserLocation {:title "location-error"})}}
      {:headerMode "none"})]
    ;id       @(work/q! queries/user-id)]
    ;(if (= -1 id) ;; default
    ; [router/router {:root Navigator :init "welcome"}]
    [router/Router {:root Navigator :init "home"}]))

(defn- reload-config!
  "takes a sequence of keys and attempts to read them from LocalStorage.
  Returns a channel with a transaction or Error"
  [ks]
  (let [data (store/load! ks)
        id   (data/q queries/user-id (work/db))
        c    (async/chan 1 (comp (map (tool/validate not-empty ::missing-data))
                                 tool/bypass-error
                                 (map #(assoc % :user/id id))))]
    (async/pipe data c)))

(defn- back-listener
  "a generic Android back button listener which pops the last element from the
  navigation stack or exists otherwise.

  Note: for Component specific back listeners it might be necessary to unsubscribe
  this listener and subscribe your own for the lifecycle of the UI component.
  See `with-let` https://reagent-project.github.io/news/news060-alpha.html"
  []
  (let [r  (data/q router/data-query (work/db))
        tx (delay (router/goBack r))]
    (cond
      (nil? (second r))
      false ;; no router initialized, Exit

      (= (tool/keywordize (first r))
         (tool/keywordize (:react.navigation/state (first @tx))))
      false ;; nothing to go back to, Exit

      :else (do (work/transact! @tx) true))))

(defn- conn-listener
  "Listens to connection changes mainly for internet access."
  [connected]
  (let [sid      (data/q queries/session (work/db))]
    (if-not connected
        (work/transact! [{:session/uuid sid :session/alert "You are offline."}]))))

(defn init!
  "register the main UI component in React Native"
  [] ;; todo: add https://github.com/reagent-project/historian
  (let [conn   (data/create-conn state/schema)
        data   (cons {:session/uuid (data/squuid)
                      :session/start (js/Date.now)}
                     state/init-data)]
    (work/init! conn)
    (work/transact! data)

    (. fl/Expo (registerRootComponent (r/reactify-component RootUi)))
    ;; handles Android BackButton
    (. fl/ReactNative (BackHandler.addEventListener
                        "hardwareBackPress"
                        back-listener))
    (. fl/ReactNative (NetInfo.isConnected.addEventListener
                        "connectionChange"
                        conn-listener))
    (let [config  (store/load! {} :user/city)
          id      (data/q queries/user-id (work/db))
          default (assoc state/defaults :user/id id)]
      (work/transact!
        (.. config
            (then (fn [m] (if (empty? m) [default] [(assoc m :user/id id)]))))))))

;(async/take! (store/delete! [:user/city]) println)

;; this also works but it is not as clear
;(async/take! (location/watch! {::location/enableHighAccuracy true
;                               ::location/timeInterval 3000})
;             cljs.pprint/pprint)

;hive.rework.state/conn
