(ns itedge.service-hub.core.convertors)

(defprotocol PEntityHandlerConvertor
  "Protocol for convertors of entities between handler and service"
  (convert-from-service-to-handler [this attributes] 
    "Converts entity coming from service to handler")
  (convert-from-handler-to-service [this attributes] 
    "Converts entity coming from handler to service"))

(defprotocol PEntityServiceConvertor
  "Protocol for convertors of entities between service and consumer"
  (convert-from-service-to-consumer [this attributes] 
    "Converts entity coming from service to consumer")
  (convert-from-consumer-to-service [this attributes] 
    "Converts entity coming from consumer to service"))
