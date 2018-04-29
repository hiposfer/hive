(ns hive.components.screens.home.welcome
  (:require [hive.queries :as queries]
            [hive.rework.core :as work]
            [oops.core :as oops]
            [hive.components.foreigns.react :as react]
            [hive.components.foreigns.expo :as expo]
            [cljs-react-navigation.reagent :as rn-nav]
            [hive.rework.util :as tool]
            [hive.foreigns :as fl]
            [clojure.string :as str]
            [cljs.spec.alpha :as s]))

(s/def ::type     #{"success"})
(s/def ::errorCode nil?)
(s/def ::id_token  string?)
(s/def ::params    (s/keys :req-un [::id_token]))
(s/def ::response  (s/keys :req-un [::type ::params ::errorCode]))

(defn latlng
  [coordinates]
  {:latitude (second coordinates) :longitude (first coordinates)})

(defn- log-in
  [domain clientId redirect]
  (let [params {:client_id clientId
                :response_type "token"
                :scope "openid profile"
                :redirect_uri redirect}
        lp   (for [[k v] params]
               (str (js/encodeURIComponent (name k)) "=" (js/encodeURIComponent v)))
        opts #js {:authUrl (str "https://" domain "/authorize?" (str/join "&" lp))}
        cb   (fn [result] (fl/JwtDecode (:id_token (:params result))))]
    (work/delay (oops/ocall fl/Expo "AuthSession.startAsync" opts)
                (comp (map tool/keywordize)
                      (map (tool/validate ::response))
                      tool/bypass-error
                      (map cb)
                      (map cljs.pprint/pprint))))) ;TODO

(defn view
  "the main screen of the app. Contains a search bar and a mapview"
  [props]
  (let [navigate (:navigate (:navigation props))
        redirectUrl  (oops/ocall fl/Expo "AuthSession.getRedirectUrl")
        [domain cid] (work/q '[:find [?domain ?id]
                               :where [_ :token/domain ?domain]
                                      [_ :token/client-id ?id]])
        id       (work/q queries/user-id)
        info    @(work/pull! [{:user/city [:city/geometry :city/bbox :city/name]}]
                             [:user/id id])
        coords   (:coordinates (:city/geometry (:user/city info)))
        dims     (tool/keywordize (oops/ocall fl/ReactNative "Dimensions.get" "window"))]
    [:> react/View {:style {:flex 1 :backgroundColor "#FFFFFF50"}}
      [:> react/View {:style {:flex 1} :pointerEvents "none"}
        [:> expo/MapView {:style {:flex 1} :showsUserLocation true
                          :region (merge (latlng coords)
                                         {:latitudeDelta 0.02 :longitudeDelta 0.02})
                          :showsMyLocationButton true}]]
     [:> react/View {:style {:position "absolute" :top (/ (:height dims) 2)
                             :left (- (/ (:width dims) 2) (/ 152 2))
                             :width 152 :height 52 :borderRadius 10
                             :backgroundColor "#FF5722" :elevation 40
                             :shadowColor "#000000" :shadowRadius 20
                             :shadowOffset {:width 0 :height 10} :shadowOpacity 1.0}}
       [:> react/TouchableOpacity {:style {:flex 1 :alignItems "center" :justifyContent "center"}
                                   :onPress #(work/transact! (log-in domain cid redirectUrl))}
         [:> react/Text {:style {:color "white" :fontWeight "bold" :fontSize 15}}
                        "Login"]]]]))

(def Screen    (rn-nav/stack-screen view {:title "welcome"}))

;hive.rework.state/conn
