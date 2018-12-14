(ns hive.screens.settings.core
  (:require [hive.state.core :as state]
            [react-native :as React]
            [hive.screens.symbols :as symbols]
            [expo :as Expo]
            [hive.state.queries :as queries]
            [reagent.core :as r]
            [cljs.spec.alpha :as s]
            [hive.services.secure-store :as secure]
            [hive.services.firebase :as firebase]
            [hive.assets :as assets]
            [hive.state.core :as state]))

(s/def ::email (s/and string? #(re-matches #"\S+@\S+\.\S+" %)))
(s/def ::password (s/and string? #(>= (count %) 8)))

(defn- UserIcon
  []
  [:> React/View {:width 50 :height 50 :margin 5 :alignItems "center"
                  :justifyContent "center" :backgroundColor "lightgray"
                  :borderRadius 50/2}
    [:> assets/Ionicons {:name "ios-person-outline" :size 30}]])

(defn- EmailInput
  [user stage]
  (let [info (state/pull! [:user/email] user)]
    [:> React/View
      (if (= :password @stage)
        [:> React/Text (:user/email @info)]
        [:> React/TextInput {:placeholder "example@domain.com" :textAlign "center"
                             :defaultValue (:user/email @info)
                             :autoCapitalize "none" :returnKeyType "next"
                             :keyboardType "email-address"
                             :onSubmitEditing #(when (s/valid? ::email (:user/email @info))
                                                 (reset! stage :password))
                             :onChangeText #(state/transact! [{:db/id user :user/email %}])
                             :style {:width 150 :height 50}}])
      (cond
        (empty? (:user/email @info)) nil

        (not (s/valid? ::email (:user/email @info)))
        [:> React/Text {:style {:color "coral"}} "invalid email"])]))

(defn- on-sign-up
  [data db]
  (when (s/valid? ::password (:user/password data))
        [[firebase/sign-in-or-up! db]
         [secure/save! (select-keys data [:user/password])]]))

(defn- PasswordInput
  [user]
  (let [info (state/pull! [:user/password] user)]
    [:> React/View
      [:> React/TextInput {:placeholder     "secret password" :textAlign "center"
                           :autoCapitalize  "none" :returnKeyType "send"
                           :onChangeText    #(state/transact! [{:db/id user :user/password %}])
                           :onSubmitEditing #(state/transact! (on-sign-up @info (state/db)))
                           :style           {:width 150 :height 50}}]
      (cond
        (empty? (:user/password @info)) nil

        (not (s/valid? ::password (:user/password @info)))
        [:> React/Text {:style {:color "coral"}} "password is too short"])]))

(defn SignUp
  [user]
  (r/with-let [info (state/pull! [:user/email :user/password] user)
               stage (r/atom :email)]
    [:> React/View {:backgroundColor "rgba(0,0,0,0.5)" :flex 1
                    :alignItems "center" :justifyContent "center"}
      [:> React/View {:backgroundColor "white" :height 300 :width 300
                      :alignItems "center" :justifyContent "space-around"
                      :borderRadius 5}
        [UserIcon]
        [EmailInput user stage]
        (when (= :password @stage)
          [PasswordInput user])
        (when (s/valid? ::password (:user/password @info))
          [:> React/TouchableOpacity {:onPress #(state/transact! (on-sign-up @info (state/db)))}
            [:> assets/Ionicons {:name "ios-checkmark" :size 26}]])]]))

(defn- UserInfo
  [user]
  (let [info     (state/pull! [:user/displayName :user/isAnonymous :user/email]
                              user)
        visible? (r/atom false)]
    (fn []
      [:> React/TouchableOpacity {:style {:height 125 :paddingBottom 20 :paddingTop 20}
                                  :onPress #(reset! visible? true)}
        (when (:user/isAnonymous @info)
          [:> React/Modal {:animationType "slide" :presentationStyle "overFullScreen"
                           :transparent true :visible @visible?
                           :onRequestClose #(reset! visible? false)}
           [SignUp user]])
        [:> React/Text {:style {:color "slategray" :fontSize 15
                                :paddingLeft 20 :justifyContent "flex-end"}}
         "USER INFO"]
        [:> React/View {:height 50 :flex-direction "row"}
          [UserIcon]
          [:> React/View {:flex 0.7 :justifyContent "center"}
           (cond
             (:user/isAnonymous @info)
             [:> React/Text "ANONYMOUS"]

             (:user/displayName @info)
             [:> React/Text (:user/displayName @info)])
           (when (not (:user/isAnonymous @info))
             [:> React/Text {:style {:color "gray"}}
              (:user/email @info)])]]])))

(defn- UserCity
  [props user-entity]
  (let [user     (state/pull! [{:user/area [:area/name]}]
                              user-entity)
        navigate (:navigate (:navigation props))]
    [:> React/View {:height 125}
     [:> React/Text {:style {:color "slategray" :fontSize 15 :paddingLeft 20
                             :justifyContent "flex-end"}}
      "CURRENT CITY"]
     [:> React/TouchableOpacity {:onPress #(navigate "select-city")
                                 :style {:height 45}}
      [symbols/PointOfInterest
       [:> assets/Ionicons {:name "md-map" :size 26}]
       [:> React/Text ""]
       [:> React/Text (:area/name (:user/area @user))]
       [:> React/Text {:style {:color "gray"}} ""]
       [:> assets/Ionicons {:name "ios-checkmark" :size 26}]]]]))

;; NOTE: here we use the entity and not the uid because the sign in
;; process might change it, thus breaking all pull patterns
(defn Settings
  [props]
  (let [user (state/q! queries/user-entity)]
    [:> React/View {:flex 1}
    ;; HEADER .....................
      [:> React/View {:height 60 :alignItems "center" :justifyContent "center"
                      :backgroundColor "blue"}
        [:> React/Text {:style {:color "white" :fontSize 20}}
                       "Settings"]]
    ;; USER INFO ...................
      [UserInfo @user]
    ;; USER CITY .................
      [UserCity props @user]]))

;hive.rework.state/conn
