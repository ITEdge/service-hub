(ns itedge.service-hub.core.authorizators)

(defprotocol PEntityServiceAuthorizator
  "A protocol for entity service authorizators"
  (authorize-find-call [this id auth]
    "Authorizes find service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-delete-call [this id auth]
    "Authorizes delete service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-update-call [this attributes auth]
    "Authorizes update service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-add-call [this attributes auth]
    "Authorizes add service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-list-call [this criteria auth]
    "Authorizes list service call, returns nil if authorization successful, otherwise returns authorization result")
  (restrict-list-call [this criteria auth]
    "Restricts list service call according to authentication info, returns restricted criteria if restriction is neccessary"))

(defprotocol PEntityHistoryServiceAuthorizator
  "A protocol for entity history service authorizators"
  (authorize-find-history-call [this entity-id history-id auth]
    "Authorizes find-history service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-list-history-call [this id criteria auth]
    "Authorizes list-entity-history service call, returns nil if authorization successful, otherwise returns authorization result")
  (restrict-list-history-call [this id criteria auth]
    "Restricts list-entity-history service call according to authentication info, returns restricted criteria if restriction is neccessary"))
