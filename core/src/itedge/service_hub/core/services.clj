(ns itedge.service-hub.core.services)

(defprotocol PEntityService
  "A protocol for entity services"
  (find-entity [this id auth] 
    "Finds entity by primary id")
  (delete-entity [this id auth] 
    "Deletes entity by primary id")
  (update-entity [this attributes auth] 
    "Updates existing entity")
  (add-entity [this attributes auth] 
    "Persists new entity")
  (list-entities [this criteria sort-attrs from to auth] 
    "Lists all entities, optionally with criteria, optionally sorts them and restricts their range"))

(defprotocol PEntityHistoryService
  "A protocol for entity-history services"
  (find-entity-history [this entity-id history-id auth]
    "Finds entity-history by entity primary id and history id")
  (list-entity-history [this id criteria sort-attrs from to auth]
    "Lists history of entity with given id, optionally with criteria, optionally sorts them and
     restricts their range"))

