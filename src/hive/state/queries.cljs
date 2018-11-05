(ns hive.state.queries)

;; TODO: most of these queries rely on user ID. It could be better to compute
;; that once and later on just pull or entity attributes out of it

;; get name geometry and bbox of each city in the db
(def cities '[:find [(pull ?entity [*]) ...]
              :where [?entity :city/name ?name]])

;; not safe to use since the user might change uid on sign in/up
;; the uid change break pull patterns based on [:user/uid v]
(def ^:deprecated user-id ;; prefer user-entity
  '[:find ?uid .
    :where [_ :user/uid ?uid]])

(def user-entity '[:find ?e . :where [?e :user/uid]])

(def user-position '[:find ?position . :where [_ :user/position ?position]])

(def places-id '[:find [?id ...] :where [?id :place/id]])

(def email&password '[:find [?email ?password]
                      :where [?user :user/uid]
                             [?user :user/email ?email]
                             [?user :user/password ?password]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def session '[:find ?session .
               :where [_ :session/uuid ?session]])

;(work/q '{:find [(pull ?city [*]) .]
;          :where [[?id :user/id]
;                  [?id :user/city ?city]]})

(def routes-ids '[:find [?routes ...]
                  :where [_ :directions/uuid ?routes]])
