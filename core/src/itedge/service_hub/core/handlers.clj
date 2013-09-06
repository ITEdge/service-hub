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
    "List all entities, optionaly with criteria, optionaly sort them and restrict their range")
  (handle-count-entities [this criteria] 
    "Count all entities, optionaly with criteria")
  (handle-get-unique-identifier [this]
    "Returns unique identifier for entity"))

(defprotocol PEntityHistoryHandler
  "A protocol for history enabled entity handlers"
  (handle-list-entity-history [this id criteria sort-attrs from to]
    "List all history states (snapshots - map of all entity atributes along with snapshot time and transaction id) of entity values, 
     optionaly with criteria, optionaly sort them and restrict their range")
  (handle-find-entity-history [this entity-id history-id]
    "Find one history state of the entity by primary id of the entity and history id (could be transaction id or time)"))
