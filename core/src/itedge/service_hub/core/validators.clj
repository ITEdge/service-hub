(ns itedge.service-hub.core.validators)

(defprotocol PEntityServiceValidator
  "A protocol for entity service validators"
  (validate-find-entity [this id]
    "Validate find operation for given entity id, returns nil if validation successful, otherwise returns validation result")
  (validate-add-entity [this attributes] 
    "Validate add operation for given attributes, returns nil if validation successful, otherwise returns validation result")
  (validate-update-entity [this attributes] 
    "Validate update operation for given attributes, 
     returns nil if validation successful, otherwise returns validation result")
  (validate-delete-entity [this id] 
    "Validate delete operation for given entity id, 
     returns nil if validation successful, otherwise returns validation result")
  (validate-list-entities [this criteria sort-attrs from to] 
    "Validate list operation for given entity, optional with criteria, sort-attrs and range boundaries, 
     returns nil if validation successful, otherwise returns validation result"))
