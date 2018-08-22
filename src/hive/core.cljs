(ns hive.core
  (:require [reagent.core :as r]
            [hive.foreigns :as fl]
            [hive.state :as state]
            [hive.rework.core :as work]
            [hive.components.screens.home.welcome :as welcome]
            [datascript.core :as data]
            [hive.services.sqlite :as sqlite]
            [hive.queries :as queries]
            [hive.rework.util :as tool]
            [hive.components.screens.home.core :as home]
            [hive.components.screens.errors :as errors]
            [hive.components.router :as router]
            [hive.components.screens.settings.core :as settings]
            [hive.components.screens.settings.city-picker :as city-picker]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.components.screens.home.route :as route]
            [hive.components.foreigns.react :as react]))

(defn- MessageTray
  [props]
  (let [id      @(work/q! queries/session)
        alert   @(work/pull! [:session/alert]
                             [:session/uuid id])]
    (when-not (empty? (:session/alert alert))
      [:> react/View {:flex 1 :justifyContent "flex-end" :alignItems "center"
                      :bottom 0 :width "100%" :height "5%" :position "absolute"}
        [:> react/Text
          {:style {:width "100%" :height "100%" :textAlign "center"
                   :backgroundColor "grey" :color "white"}
           :onPress #(work/transact! [{:session/uuid id :session/alert {}}])}
          (:session/alert alert)]])))

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
  (let [Navigator (rn-nav/stack-navigator
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

(defn- back-listener!
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

      :else (some? (work/transact! @tx))))) ;; always returns true

(defn- conn-listener
  "Listens to connection changes mainly for internet access."
  [connected]
  (let [sid @(work/q! queries/session)]
    (if-not connected
        (work/transact! [{:session/uuid sid :session/alert "You are offline."}]))))

(defn- auth-listener
  [db result]
  (let [e (data/q '[:find ?e :where [?e :user/uid]] db)
        user (js->clj result :keywordize-keys true)]
    [(merge user {:db/id e})]))

(defn init!
  "register the main UI component in React Native"
  []
  (let [conn       (data/create-conn state/schema)
        config #js {:apiKey (:ENV/FIREBASE_API_KEY state/tokens)
                    :authDomain (:ENV/FIREBASE_AUTH_DOMAIN state/tokens)
                    :databaseUrl (:ENV/FIREBASE_DATABASE_URL state/tokens)
                    :storageBucket (:ENV/FIREBASE_STORAGE_BUCKET state/tokens)}]
    (work/init! conn)
    (sqlite/listen! conn)
    (work/transact! state/init-data)
    (work/transact! [{:session/uuid (data/squuid)
                      :session/start (js/Date.now)}])
    ;;(. fl/Firebase (initializeApp config))
    ;(.. fl/Firebase
    ;    (auth)
    ;    (onAuthStateChanged #(work/transact! (auth-listener (work/db) %))))
    ;(.. fl/Firebase
    ;    (auth)
    ;    (signInAnonymously) (catch js/console.error))
    ;; restore user data ...........................
    (. (sqlite/read!) (then work/transact!))
    ;; start listening for events ..................
    (. fl/Expo (registerRootComponent (r/reactify-component RootUi)))
    ;; handles Android BackButton
    (. fl/ReactNative (BackHandler.addEventListener
                        "hardwareBackPress"
                        back-listener!))
    (. fl/ReactNative (NetInfo.isConnected.addEventListener
                        "connectionChange"
                        conn-listener))))
;hive.rework.state/conn

;(work/transact! [{:db/id 4
;                  :user/id 100}])
;(work/transact! [[:db.fn/retractEntity 7]])

;(js->clj (.. fl/Firebase (auth))
;         :keywordize-keys true)))

;(.. fl/Firebase (auth) -currentUser)
