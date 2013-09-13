(ns itedge.service-hub.core.validators)

(defprotocol PEntityServiceValidator
  "A protocol for entity service validators"
  (validate-find-entity [this id]
    "Validates find operation for given entity id, returns nil if validation successful, otherwise returns validation result")
  (validate-add-entity [this attributes] 
    "Validates add operation for given attributes, returns nil if validation successful, otherwise returns validation result")
  (validate-update-entity [this attributes] 
    "Validates update operation for given attributes, 
     returns nil if validation successful, otherwise returns validation result")
  (validate-delete-entity [this id] 
    "Validates delete operation for given entity id, 
     returns nil if validation successful, otherwise returns validation result")
  (validate-list-entities [this criteria sort-attrs from to] 
    "Validates list operation for given entity, optional with criteria, sort-attrs and range boundaries, 
     returns nil if validation successful, otherwise returns validation result"))

(defprotocol PEntityHistoryServiceValidator
  "A protocol for entity history-service validators"
  (validate-find-entity-history [this entity-id history-id]
    "Validates find-entity-history operation for given entity id and history id, returns nil if validation successful,
     otherwise returns validation result")
  (validate-list-entity-history [this id criteria sort-attrs from to]
    "Validates list-entity-history operation for given entity and entity id, optional with criteria, sort-attrs
     and range boundaries, returns nil if validation successful, otherwise returns validation result"))
