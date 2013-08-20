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
