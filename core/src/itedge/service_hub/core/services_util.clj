(ns itedge.service-hub.core.services-util
  (:require [itedge.service-hub.core.services :refer :all]
            [itedge.service-hub.core.handlers :refer :all]
            [itedge.service-hub.core.validators :refer :all]
            [itedge.service-hub.core.authorizators :refer :all]
            [itedge.service-hub.core.util :as util]))

(defn handle-service-exception
  "Returns exception wrapped in standard service-result map"
  [e]
  (util/get-service-result :error (map #(.toString %) (.getStackTrace e))))

(defn- service-validator
  "Executes service and validator functions in try-catch block"
  [service-fns validator-fn]
  `(try 
     (let [validator-result# ~validator-fn]
       (if (nil? validator-result#) 
         ~service-fns
         validator-result#))
     (catch Exception e# (handle-service-exception e#))))  
  
(defmacro get-service-result
  "Macro which returns service-result, arguments include
   service function, optional validator function and optional locking object"
  ([service-fns]
     (service-validator service-fns nil))
  ([validator-fn service-fns ]
     (service-validator service-fns validator-fn))
  ([validator-fn service-fns lock-object]
     `(locking ~lock-object ~(service-validator validator-fn service-fns))))

(defmacro authorize-service-call
  "Macro which authorizes service-call"
  [authorizator-fn service-call]
  (service-validator service-call authorizator-fn))

(defmacro do-side-effects
  "Macro which wraps some function, executes this function, does defined side effects and returns result from this function"
  [fn & side-effects]
  `(let [fn-result# ~fn]
     ~@side-effects
     fn-result#))

(defn get-success-response
  "Function which creates standard success response for handler result"
  [handler-result]
  (util/get-service-result :ok handler-result))

(defn get-success-delete-response
  "Function which creates standard success-delete response for handler result"
  [handler-result]
  (util/get-service-result :delete-ok handler-result))

(defn assoc-range-info
  "Function which associates range info to the response"
  [response from to total]
  (let [from (or from 0)
        to (or to (dec total))]
    (-> (assoc response :from from)
        (assoc :to (if (> (inc to) total) (dec total) to))
        (assoc :total total))))

(defn scaffold-find-call
  "Scaffolds service find call"
  ([handler id db-value]
     (get-success-response
      (handle-find-entity handler id db-value)))
  ([handler validator id db-value]
     (get-service-result
      (validate-find-entity validator id db-value)
      (scaffold-find-call handler id db-value)))
  ([handler validator authorizator id db-value auth]
     (authorize-service-call
      (authorize-find-call authorizator id db-value auth)
      (scaffold-find-call handler validator id db-value))))

(defn scaffold-delete-call
  "Scaffolds service delete call"
  ([handler id db-handle]
     (get-success-delete-response
      (handle-delete-entity handler id db-handle)))
  ([handler validator id db-value db-handle]
     (get-service-result
      (validate-delete-entity validator id db-value)
      (scaffold-delete-call handler id db-handle)))
  ([handler validator authorizator id db-value db-handle auth]
     (authorize-service-call
      (authorize-delete-call authorizator id db-value auth)
      (scaffold-delete-call handler validator id db-value db-handle))))

(defn scaffold-update-call
  "Scaffolds service update call"
  ([handler attributes db-handle]
     (get-success-response
      (handle-update-entity handler attributes db-handle)))
  ([handler validator attributes db-value db-handle]
     (get-service-result
      (validate-update-entity validator attributes db-value)
      (scaffold-update-call handler attributes db-handle)))
  ([handler validator authorizator attributes db-value db-handle auth]
     (authorize-service-call
      (authorize-update-call authorizator attributes db-value auth)
      (scaffold-update-call handler validator attributes db-value db-handle))))

(defn scaffold-add-call
  "Scaffolds service add call"
  ([handler attributes db-handle]
     (get-success-response
      (handle-add-entity handler attributes db-handle)))
  ([handler validator attributes db-value db-handle]
     (get-service-result
      (validate-add-entity validator attributes db-value)
      (scaffold-add-call handler attributes db-handle)))
  ([handler validator authorizator attributes db-value db-handle auth]
     (authorize-service-call
      (authorize-add-call authorizator attributes db-value auth)
      (scaffold-add-call handler validator attributes db-value db-handle))))

(defn scaffold-list-call
  "Scaffolds service list call"
  ([handler criteria sort-attrs from to db-value]
     (-> (get-success-response 
          (handle-list-entities handler criteria sort-attrs from to db-value))
         (assoc-range-info from to (handle-count-entities handler criteria db-value))))
  ([handler validator criteria sort-attrs from to db-value]
     (get-service-result
      (validate-list-entities validator criteria sort-attrs from to db-value)
      (scaffold-list-call handler criteria sort-attrs from to db-value)))
  ([handler validator authorizator criteria sort-attrs from to db-value auth]
     (authorize-service-call
      (authorize-list-call authorizator criteria db-value auth)
      (let [criteria (restrict-list-call authorizator criteria db-value auth)]
        (scaffold-list-call handler validator criteria sort-attrs from to db-value)))))

(defn scaffold-get-history-call
  "Scaffolds service get-history call"
  ([handler entity-id history-id db-value]
     (get-success-response
      (handle-find-entity-history handler entity-id history-id db-value)))
  ([handler validator entity-id history-id db-value]
     (get-service-result
      (validate-find-entity-history validator entity-id history-id db-value)
      (scaffold-get-history-call handler entity-id history-id db-value)))
  ([handler validator authorizator entity-id history-id db-value auth]
     (authorize-service-call
      (authorize-find-history-call authorizator entity-id history-id db-value auth)
      (scaffold-get-history-call handler validator entity-id history-id db-value))))

(defn scaffold-list-entity-history-call
  "Scaffolds service list-entity-history call"
  ([handler id criteria sort-attrs from to db-value]
     (-> (get-success-response
          (handle-list-entity-history handler id criteria sort-attrs from to db-value))
         (assoc-range-info from to (handle-count-entity-history handler id criteria db-value))))
  ([handler validator id criteria sort-attrs from to db-value]
     (get-service-result
      (validate-list-entity-history validator id criteria sort-attrs from to db-value)
      (scaffold-list-entity-history-call handler id criteria sort-attrs from to db-value)))
  ([handler validator authorizator id criteria sort-attrs from to db-value auth]
     (authorize-service-call
      (authorize-list-history-call authorizator id criteria db-value auth)
      (let [criteria (restrict-list-history-call authorizator id criteria db-value auth)]
        (scaffold-list-entity-history-call handler validator id criteria sort-attrs from to db-value)))))

(defn scaffold-service
  "Scaffolds simple service implementation with mandatory handler and optional
   validator and authorizator arguments"
  ([handler db-handle get-db-value-fn]
     (reify PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler id (get-db-value-fn)))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler id db-handle))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler attributes db-handle))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler attributes db-handle))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler criteria sort-attrs from to (get-db-value-fn)))))
  ([handler validator db-handle get-db-value-fn]
     (reify PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler validator id (get-db-value-fn)))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler validator id (get-db-value-fn) db-handle))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler validator attributes (get-db-value-fn) db-handle))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler validator attributes (get-db-value-fn) db-handle))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler validator criteria sort-attrs from to (get-db-value-fn)))))
  ([handler validator authorizator db-handle get-db-value-fn]
     (reify PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler validator authorizator id (get-db-value-fn) auth))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler validator authorizator id (get-db-value-fn) db-handle auth))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler validator authorizator attributes (get-db-value-fn) db-handle auth))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler validator authorizator attributes (get-db-value-fn) db-handle auth))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler validator authorizator criteria sort-attrs from to (get-db-value-fn) auth)))))

(defn scaffold-history-enabled-service
  "Scaffolds simple history-enabled service implementation with mandatory handler 
   and optional validator and authorizator arguments"
  ([handler db-handle get-db-value-fn]
     (reify 
       PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler id (get-db-value-fn)))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler id db-handle))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler attributes db-handle))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler attributes db-handle))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler criteria sort-attrs from to (get-db-value-fn)))
       PEntityHistoryService
       (find-entity-history [_ entity-id history-id auth]
         (scaffold-get-history-call handler entity-id history-id (get-db-value-fn)))
       (list-entity-history [_ id criteria sort-attrs from to auth]
         (scaffold-list-entity-history-call handler id criteria sort-attrs from to (get-db-value-fn)))))
  ([handler validator db-handle get-db-value-fn]
     (reify 
       PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler validator id (get-db-value-fn)))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler validator id (get-db-value-fn) db-handle))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler validator attributes (get-db-value-fn) db-handle))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler validator attributes (get-db-value-fn) db-handle))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler validator criteria sort-attrs from to (get-db-value-fn)))
       PEntityHistoryService
       (find-entity-history [_ entity-id history-id auth]
         (scaffold-get-history-call handler validator entity-id history-id (get-db-value-fn)))
       (list-entity-history [_ id criteria sort-attrs from to auth]
         (scaffold-list-entity-history-call handler validator id criteria sort-attrs from to (get-db-value-fn)))))
  ([handler validator authorizator db-handle get-db-value-fn]
     (reify 
       PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler validator authorizator id (get-db-value-fn) auth))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler validator authorizator id (get-db-value-fn) db-handle auth))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler validator authorizator attributes (get-db-value-fn) db-handle auth))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler validator authorizator attributes (get-db-value-fn) db-handle auth))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler validator authorizator criteria sort-attrs from to (get-db-value-fn) auth))
       PEntityHistoryService
       (find-entity-history [_ entity-id history-id auth]
         (scaffold-get-history-call handler validator authorizator entity-id history-id (get-db-value-fn) auth))
       (list-entity-history [_ id criteria sort-attrs from to auth]
         (scaffold-list-entity-history-call handler validator authorizator id criteria sort-attrs from to (get-db-value-fn) auth)))))
