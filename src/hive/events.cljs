(ns hive.events)

;; -- Handlers --------------------------------------------------------------

(defn- start-services
  [cofx [_ secrets]]
  (let [tokens (js->clj secrets :keywordize-keys true)]
    {:db (assoc (:db cofx) :tokens tokens
                           :view/screen :home)
     :mapbox/init (:mapbox tokens)
     :firebase/init (:firebase tokens)}))

(defn on-back-button
  "modifies the behavior of the back button on Android according to the view
  currently active. It defaults to Exit app if no screen was found"
  [cofx _]
  (case (:view/screen (:db cofx))
    :home (if (:view.home/targets (:db cofx))
            {:db (assoc (:db cofx) :view.home/targets false)}
            {:app/exit true})
    :setting {:db (assoc (:db cofx) :view/screen :home)}
    :directions {:db (assoc (:db cofx) :view/screen :home)}
    {:app/exit true}))

(defn assoc-rf
  "basic event handler. It assocs the event id with its value"
  [db [id v]] (assoc db id v))

(defn update-user-city [db [id v]] (assoc db id v :map/camera [(:center v)]))

(defn on-search-place
  "store the current user search text and trigger a geocode for it"
  [cofx [_ text]]
  (if (empty? text)
    {:db (assoc (:db cofx) :user.input/place "" :view.home/targets false)
     :user.input/clear (:user.input/ref (:db cofx))}
    {:db (assoc (:db cofx) :user.input/place text)
     :dispatch [:map.geocode/mapbox text :map/annotations]}))

(defn on-internet-state
  [cofx [id enabled?]]
  (cond
    (and (not (:app/internet (:db cofx))) enabled?) {:dispatch [:hive/state]}
    (not enabled?) {:db (assoc (:db cofx) id enabled? :view/screen :404)}
    :otherwise {}))

;;(cljs.pprint/pprint base)