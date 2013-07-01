(ns itedge.service-hub.core.services)

(defprotocol PEntityService
  "A protocol for entity services"
  (find-entity [this id auth] 
    "Find entity by primary id")
  (delete-entity [this id auth] 
    "Delete entity by primary id")
  (update-entity [this attributes auth] 
    "Update existing entity")
  (add-entity [this attributes auth] 
    "Persist new entity")
  (list-entities [this criteria sort-attrs from to auth] 
    "List all entities, optionally with criteria, optionaly sort them and restrict their range"))

