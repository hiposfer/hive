(ns hive.state.queries)

;; TODO: most of these queries rely on user ID. It could be better to compute
;; that once and later on just pull or entity attributes out of it

;; get name geometry and bbox of each city in the db
(def kamal-areas '[:find [?area] :where [?area :area/id]])

;; not safe to use since the user might change uid on sign in/up
;; the uid change break pull patterns based on [:user/uid v]
(def user-id "DEPRECATED - prefer user-entity"
  '[:find ?uid .
    :where [_ :user/uid ?uid]])

(def user-area-id '[:find ?area-id . :where [?user :user/uid]
                                            [?user :user/area ?area]
                                            [?area :area/id ?area-id]])

(def user-area-bbox '[:find ?bbox . :where [?id :user/uid]
                                           [?id :user/area ?area]
                                           [?area :area/bbox ?bbox]])

(def user-entity '[:find ?e . :where [?e :user/uid]])

(def user-goal '[:find ?e . :where [?user :user/uid]
                                   [?user :user/goal ?e]])

(def user-route '[:find ?route .
                  :where [_ :user/directions ?route]])

(def user-position '[:find ?position . :where [?user :user/uid]
                                              [?user :user/position ?position]])

(def places-id '[:find [?id ...] :where [?id :place/id]])

(def email&password '[:find [?email ?password]
                      :where [?user :user/uid]
                             [?user :user/email ?email]
                             [?user :user/password ?password]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def session '[:find ?session .
               :where [_ :session/uuid ?session]])

(def routes-ids '[:find [?routes ...]
                  :where [_ :directions/uuid ?routes]])
