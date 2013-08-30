(ns itedge.service-hub.persistence-datomic.handlers-datomic
  (:require [datomic.api :as d :refer [db]]
            [itedge.service-hub.persistence-datomic.util :as datomic-util]
            [itedge.service-hub.core.handlers :refer :all]))

(defprotocol PRealizable
  "Every type implementing this protocol should return clojure associative datastructure 
   (implementing IPersistentMap) representing entity data associated with this type fieldset and db, 
   which could be for example numeric or string primary key or lazy  map of entity attributes"
  (get-entity-data [information fieldset db]))

(extend-type datomic.Entity
  PRealizable
  (get-entity-data [dynamic-map fieldset db]
    (datomic-util/convert-entity-to-map dynamic-map)))

(extend-type java.lang.Object
  PRealizable
  (get-entity-data [id fieldset db]
    (datomic-util/get-entity db fieldset id)))

(extend-type nil
  PRealizable
  (get-entity-data [id fieldset db]
    nil))

(defn create-handler [conn fieldset]
  (reify PEntityHandler
    (handle-find-entity [_ id]
      (get-entity-data id fieldset (db conn)))
    (handle-exist-entity [_ id]
      (datomic-util/exist-entity? (db conn) fieldset id))
    (handle-delete-entity [_ id]
      (datomic-util/delete-entity conn fieldset id))
    (handle-update-entity [_ attributes]
      (datomic-util/update-entity conn attributes))
    (handle-add-entity [_ attributes]
      (datomic-util/add-entity conn attributes))
    (handle-list-entities [_ criteria sort-attrs from to]
      (datomic-util/list-entities (db conn) fieldset criteria sort-attrs from to))
    (handle-count-entities [_ criteria]
      (datomic-util/count-entities (db conn) fieldset criteria))
    (handle-get-unique-identifier [_]
      :db/id)))
