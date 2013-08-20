(ns itedge.service-hub.core.handlers)

(defprotocol PEntityHandler
  "A protocol for entity handlers"
  (handle-find-entity [this id] 
    "Find entity by primary id")
  (handle-exist-entity [this id]
    "Determines if entity with given primary id exist")
  (handle-delete-entity [this id] 
    "Delete entity by primary id")
  (handle-update-entity [this attributes] 
    "Update existing entity")
  (handle-add-entity [this attributes] 
    "Persist new entity")
  (handle-list-entities [this criteria sort-attrs from to] 
    "List all entities, optionally with criteria, optionaly sort them, and restrict their range")
  (handle-count-entities [this criteria] 
    "Count all entities, optionally with criteria")
  (handle-get-unique-identifier [this]
    "Returns unique identifier for entity"))
