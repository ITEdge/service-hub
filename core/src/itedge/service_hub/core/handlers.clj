(ns itedge.service-hub.core.handlers)

(defprotocol PEntityHandler
  "A protocol for entity handlers"
  (handle-find-entity [this id db-value]
    "Finds entity by primary id")
  (handle-exist-entity [this id db-value]
    "Determines if entity with given primary id exists")
  (handle-delete-entity [this id db-handle] 
    "Deletes entity by primary id")
  (handle-update-entity [this attributes db-handle] 
    "Updates existing entity")
  (handle-add-entity [this attributes db-handle] 
    "Persists new entity")
  (handle-list-entities [this criteria sort-attrs from to db-value]
    "Lists all entities, optionally with criteria, optionaly sort them and restrict their range")
  (handle-count-entities [this criteria db-value]
    "Counts all entities, optionally with criteria")
  (handle-get-unique-identifier [this]
    "Returns unique identifier for entity"))

(defprotocol PEntityHistoryHandler
  "A protocol for history enabled entity handlers"
  (handle-find-entity-history [this entity-id history-id db-value]
    "Finds one history state of the entity by primary id of the entity and history id (could be transaction id or time)")
  (handle-exist-entity-history [this entity-id history-id db-value]
    "Determines if entity history for given entity-id and history-id exists")
  (handle-list-entity-history [this id criteria sort-attrs from to db-value]
    "Lists all history states (snapshots - map of all entity attributes along with snapshot time and transaction id) of entity values,
     optionally with criteria, optionaly sort them and restrict their range")
  (handle-count-entity-history [this id criteria db-value]
    "Counts all entity history states, optionally with criteria"))
