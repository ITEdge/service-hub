(ns itedge.service-hub.common.handlers)

(defprotocol PUserHandler
  "A protocol for user handler"
  (handle-find-by-username [this username] 
    "Find user by username"))

(defprotocol PConfigurationHandler
  "A protocol for configuration handler"
  (handle-get-property [this prop-name]
    "Get configuration property")
  (handle-get-num-property [this prop-name defaults]
    "Find numeric property with optional default value")
  (handle-get-txt-property [this prop-name defaults]
    "Find textual property with optional default value")
  (handle-get-dat-property [this prop-name defaults]
    "Find date property with optional default value"))
