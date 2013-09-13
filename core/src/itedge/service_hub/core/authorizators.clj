(ns itedge.service-hub.core.authorizators)

(defprotocol PEntityServiceAuthorizator
  "A protocol for entity service authorizators"
  (authorize-find-call [this id auth]
    "Authorize find service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-delete-call [this id auth]
    "Authorize delete service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-update-call [this attributes auth]
    "Authorize update service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-add-call [this attributes auth]
    "Authorize add service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-list-call [this criteria auth]
    "Authorize list service call, returns nil if authorization successful, otherwise returns authorization result")
  (restrict-list-call [this criteria auth]
    "Restrict list service call according to authentication info, returns restricted criteria if restriction is neccessary"))

(defprotocol PEntityHistoryServiceAuthorizator
  "A protocol for entity history service authorizators"
  (authorize-find-history-call [this entity-id history-id auth]
    "Authorize find-history service call, returns nil if authorization successful, otherwise returns authorization result")
  (authorize-list-entity-history-call [this id criteria auth]
    "Authorize list-entity-history service call, returns nil if authorization successful, otherwise returns authorization result"))
