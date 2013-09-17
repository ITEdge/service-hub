(ns itedge.service-hub.persistence-datomic.handlers-datomic
  (:require [datomic.api :as d :refer [db]]
            [itedge.service-hub.core.util :as util]
            [itedge.service-hub.persistence-datomic.util :as datomic-util]
            [itedge.service-hub.core.handlers :refer :all]))

(defprotocol PRealizable
  "Protocol for datomic handler arguments"
  (get-entity-data [information fieldset db] "Get entity data in form of associative map structure")
  (resolve-id-value [information] "Resolve id value"))

(extend-type datomic.Entity
  PRealizable
  (get-entity-data [dynamic-map fieldset db]
    (datomic-util/convert-entity-to-map dynamic-map))
  (resolve-id-value [dynamic-map] 
    (:db/id dynamic-map)))

(extend-type java.lang.Object
  PRealizable
  (get-entity-data [id fieldset db]
    (datomic-util/get-entity db fieldset id))
  (resolve-id-value [id] id))

(extend-type nil
  PRealizable
  (get-entity-data [id fieldset db] nil)
  (resolve-id-value [id] nil))

(defn create-handler [fieldset]
  (reify 
    PEntityHandler
    (handle-find-entity [_ id db-value]
      (get-entity-data id fieldset db-value))
    (handle-exist-entity [_ id db-value]
      (datomic-util/exist-entity? db-value fieldset (resolve-id-value id)))
    (handle-delete-entity [_ id conn]
      (datomic-util/delete-entity conn fieldset (resolve-id-value id)))
    (handle-update-entity [_ attributes conn]
      (datomic-util/update-entity conn (util/update-map-values attributes resolve-id-value)))
    (handle-add-entity [_ attributes conn]
      (datomic-util/add-entity conn (util/update-map-values attributes resolve-id-value)))
    (handle-list-entities [_ criteria sort-attrs from to db-value]
      (datomic-util/list-entities db-value fieldset (util/update-map-values criteria resolve-id-value) :db/id sort-attrs from to))
    (handle-count-entities [_ criteria db-value]
      (datomic-util/count-entities db-value fieldset (util/update-map-values criteria resolve-id-value) :db/id))
    (handle-get-unique-identifier [_]
      :db/id)
    PEntityHistoryHandler
    (handle-find-entity-history [_ entity-id history-id db-value]
      (datomic-util/get-entity-history db-value fieldset (resolve-id-value entity-id) history-id))
    (handle-exist-entity-history [_ entity-id history-id db-value]
      (datomic-util/exist-entity-history? db-value fieldset (resolve-id-value entity-id) history-id))
    (handle-list-entity-history [_ id criteria sort-attrs from to db-value]
      (datomic-util/list-entities-with-history 
       db-value fieldset (assoc (util/update-map-values criteria resolve-id-value) :db/id (resolve-id-value id)) 
       :db/id sort-attrs from to))
    (handle-count-entity-history [_ id criteria db-value]
      (datomic-util/count-history-entities 
       db-value fieldset (assoc (util/update-map-values criteria resolve-id-value) :db/id (resolve-id-value id))
       :db/id))))
