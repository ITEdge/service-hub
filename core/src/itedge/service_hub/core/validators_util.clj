(ns itedge.service-hub.core.validators-util
  (:require [itedge.service-hub.core.handlers :refer :all]
            [itedge.service-hub.core.util :as util]
            [itedge.service-hub.core.date-time-util :as date-time-util]))

(defn- empty-val?
  "Determines if map value is empty - nil"
  [v]
  (nil? (val v)))

(defn validate-insert-fields
  "Validates mandatory fields for insertion of given entity"
  [attributes mandatory-set]
  (let [mandatory-map (select-keys attributes mandatory-set)]
    (when (or (not (= (count mandatory-set) (count mandatory-map))) (some empty-val? mandatory-map))
      (util/get-service-result :conflict "one or more mandatory fields are missing or empty"))))

(defn validate-update-fields
  "Validates mandatory fields for update of given entity"
  [attributes mandatory-set]
  (let [mandatory-map (select-keys attributes mandatory-set)]
    (when (some empty-val? mandatory-map)
      (util/get-service-result :conflict "one or more mandatory fields have null values"))))

(defn- relations-not-exist
  "Determines if given relations exist"
  [relations entity-handler]
  (some 
    (fn [item]
      (not (handle-exist-entity entity-handler (if (map? item) (:id item) item))))
        (if (vector? relations) relations (vector relations))))

(defn validate-insert-update-relations
  "Validates relation for insertion or update of given entity"
  [attributes entity-key entity-handler]
  (when-let [related-value (entity-key attributes)]
    (when (relations-not-exist related-value entity-handler)
      (util/get-service-result :conflict "mandatory related entity with given primary key does not exist"))))

(defn validate-mandatory-insert-relations
  "Validates mandatory relation for insertion of given entity"
  [attributes entity-key entity-handler]
  (if-let [related-value (entity-key attributes)]
    (when (relations-not-exist related-value entity-handler)
      (util/get-service-result :conflict "mandatory related entity with given primary key does not exist"))
    (util/get-service-result :conflict "mandatory related entity is not present")))

(defn validate-unique-fields
  "Validates unique fields among existing entities"
  [attributes entity-handler unique-set]
  (let [unique-map (select-keys attributes unique-set)]
    (when (some (fn [e]
                  (let [criteria {(key e) (val e)}
                        pk (handle-get-unique-identifier entity-handler)
                        final-criteria (if (pk attributes) (assoc criteria pk {:not (pk attributes)}) criteria)]
                    (not (zero? (handle-count-entities entity-handler final-criteria))))) unique-map)
      (util/get-service-result :conflict "one or more unique values in entity are not unique among other entities"))))

(defn validate-entity-still-there
  "Validates if given entity still exists in system for update operations"
  [attributes entity-handler]
  (let [pk (handle-get-unique-identifier entity-handler)]
	  (if-let [id (pk attributes)]
	    (when (not (handle-exist-entity entity-handler id))
	      (util/get-service-result :gone "requested entity does not exist anymore, it was probably deleted by another user"))
	    (util/get-service-result :conflict "requested entity does not posses id, which is required to check it's availability"))))

(defn validate-entity-present
  "Validates if given entity is present in system"
  [id entity-handler]
  (when (not (handle-exist-entity entity-handler id))
    (util/get-service-result :not-found "entity with requested id was not found")))

(defn validate-delete-relations
  "Validates if there are any conflicts with other entities on entity scheduled for deletion"
  [id entity-key conflict-entity-handler]
  (when (not (zero? (handle-count-entities conflict-entity-handler {entity-key id})))
    (util/get-service-result :conflict "entity requested for deletion has mandatory relations to other entities")))

(defn- validate-range
  "Validates requested range"
  [from to total]
  (if (> from to)
    (util/get-service-result :conflict "Wrong range selection, 'from' must be smaller then 'to' boundary")
    (when (> from total)
      (util/get-service-result :bad-range "Total count of given entities in system is smaller than lower range restriction"))))

(defn validate-list-range
  "Validates requested range for query operation"
  [from to criteria entity-handler]
  (when (and from to)
    (validate-range from to (handle-count-entities entity-handler criteria))))

(defn validate-implication-insert
  "Validates implication of two properties for insert operation, eq. if first property is present and not null, 
   second must be present and not null too"
  [attributes first second]
  (when (first attributes)
    (when (not (second attributes))
      (util/get-service-result :conflict "Implicative properties relation was violated"))))

(defn validate-implication-update
  "Validates implication of two properties for update operation, eq if first property not null, 
   second must be not null too"
  [attributes entity-handler first second]
  (let [id ((handle-get-unique-identifier entity-handler) attributes)
        attributes (merge (handle-find-entity entity-handler id) attributes)]
    (validate-implication-insert attributes first second)))

(defn validate-date-times-chronology
  "Validates two dates, sooner and later"
  [attributes entity-handler sooner-key later-key]
  (let [sooner (sooner-key attributes)
        later (later-key attributes)
        converted-sooner (when sooner (date-time-util/iso-8061-to-datetime sooner))
        converted-later (when later (date-time-util/iso-8061-to-datetime later))
        decision-fn (fn [sooner-dat later-dat]
                      (when (.isBefore later-dat (.plusMinutes sooner-dat 1))
                        (util/get-service-result :conflict "there is conflict in date-times chronology")))]
    (if (and converted-sooner converted-later)
      (decision-fn converted-sooner converted-later)
      (when-let [id ((handle-get-unique-identifier entity-handler) attributes)]
        (let [attributes (handle-find-entity entity-handler id)
              converted-sooner (or converted-sooner (sooner-key attributes))
              converted-later (or converted-later (later-key attributes))]
          (if (and converted-sooner converted-later)
            (decision-fn converted-sooner converted-later)))))))

(defn validate-iso-date-times
  "Validates date-time fields if they conform to ISO-8061 specification"
  [attributes date-time-set]
  (let [date-time-map (select-keys attributes date-time-set)]
    (when (some #(and (not (nil? %)) (not (date-time-util/iso-8061? (val %)))) date-time-map)
      (util/get-service-result :conflict "some date-time fields don't conform to ISO-8061 specification"))))
