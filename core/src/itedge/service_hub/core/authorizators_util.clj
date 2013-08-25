(ns itedge.service-hub.core.authorizators-util
  (:require [itedge.service-hub.core.handlers :refer :all]
            [clojure.set :as set]
            [itedge.service-hub.core.util :as util]
            [itedge.service-hub.core.security :as security]))

(defn authenticated?
  "Determines if current user is authenticated"
  [auth]
  (when-not (map? auth)
    (util/get-service-result :not-authenticated "authentication is required for this service operation")))

(defn authorized?
  "Determines if current user is authorized according to required roles"
  [auth roles]
  (when (not (security/authorized? roles auth))
    (util/get-service-result :not-authorized "user is not authorized for this service operation")))

(defn restrict-user-relations
  "Restricts query to entities related to current authenticated user"
  [{id :id} criteria user-key]
  (assoc criteria user-key id))

(defn entity-contains?
  "Determines if single entity possesses specified id or if collection of entities has one which possesses it"
  [entity-id id]  
  (if (sequential? entity-id)
    (if (util/in? entity-id id)
      true
      false)
    (if (= id entity-id)
      true
      false)))

(defn user-relations-on-entity?
  "Checks user relations on entity"
  [{id :id} entity-id user-key entity-handler]
  (when-let [entity (handle-find-entity entity-handler entity-id)]
    (when (not (entity-contains? (user-key entity) id))
      (util/get-service-result :not-authorized "user is not authorized to operate on this entity"))))

(defn contains-roles?
  "Checks if provided auth info contains some of the specified roles"
  [{roles :roles} required-roles]
  (if (empty? (set/intersection roles required-roles))
    false
    true))
