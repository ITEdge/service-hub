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
  ([handler id]
   (get-success-response
     (handle-find-entity handler id)))
  ([handler validator id]
   (get-service-result
     (validate-find-entity validator id)
     (scaffold-find-call handler id)))
  ([handler validator authorizator id auth]
   (authorize-service-call
     (authorize-find-call authorizator id auth)
     (scaffold-find-call handler validator id))))

(defn scaffold-delete-call
  "Scaffolds service delete call"
  ([handler id]
   (get-success-delete-response
     (handle-delete-entity handler id)))
  ([handler validator id]
   (get-service-result
     (validate-delete-entity validator id)
     (scaffold-delete-call handler id)))
  ([handler validator authorizator id auth]
   (authorize-service-call
     (authorize-delete-call authorizator id auth)
     (scaffold-delete-call handler validator id))))

(defn scaffold-update-call
  "Scaffolds service update call"
  ([handler attributes]
   (get-success-response
     (handle-update-entity handler attributes)))
  ([handler validator attributes]
   (get-service-result
     (validate-update-entity validator attributes)
     (scaffold-update-call handler attributes)))
  ([handler validator authorizator attributes auth]
   (authorize-service-call
     (authorize-update-call authorizator attributes auth)
     (scaffold-update-call handler validator attributes))))

(defn scaffold-add-call
  "Scaffolds service add call"
  ([handler attributes]
   (get-success-response
     (handle-add-entity handler attributes)))
  ([handler validator attributes]
   (get-service-result
     (validate-add-entity validator attributes)
     (scaffold-add-call handler attributes)))
  ([handler validator authorizator attributes auth]
   (authorize-service-call
     (authorize-add-call authorizator attributes auth)
     (scaffold-add-call handler validator attributes))))

(defn scaffold-list-call
  "Scaffolds service list call"
  ([handler criteria sort-attrs from to]
   (-> (get-success-response 
         (handle-list-entities handler criteria sort-attrs from to))
       (assoc-range-info from to (handle-count-entities handler criteria))))
  ([handler validator criteria sort-attrs from to]
   (get-service-result
     (validate-list-entities validator criteria sort-attrs from to)
     (scaffold-list-call handler criteria sort-attrs from to)))
  ([handler validator authorizator criteria sort-attrs from to auth]
   (authorize-service-call
     (authorize-list-call authorizator criteria auth)
     (let [criteria (restrict-list-call authorizator criteria auth)]
       (scaffold-list-call handler validator criteria sort-attrs from to)))))

(defn scaffold-service
  "Scaffolds simple service implementation with mandatory handler and optional
   validator and authorizator arguments"
  ([handler]
   (reify PEntityService
     (find-entity [_ id auth]
       (scaffold-find-call handler id))
     (delete-entity [_ id auth]
       (scaffold-delete-call handler id))
     (update-entity [_ attributes auth]
       (scaffold-update-call handler attributes))
     (add-entity [_ attributes auth]
       (scaffold-add-call handler attributes))
     (list-entities [_ criteria sort-attrs from to auth]
       (scaffold-list-call handler criteria sort-attrs from to))))
  ([handler validator]
   (reify PEntityService
     (find-entity [_ id auth]
       (scaffold-find-call handler validator id))
     (delete-entity [_ id auth]
       (scaffold-delete-call handler validator id))
     (update-entity [_ attributes auth]
       (scaffold-update-call handler validator attributes))
     (add-entity [_ attributes auth]
       (scaffold-add-call handler validator attributes))
     (list-entities [_ criteria sort-attrs from to auth]
       (scaffold-list-call handler validator criteria sort-attrs from to))))
  ([handler validator authorizator]
   (reify PEntityService
     (find-entity [_ id auth]
       (scaffold-find-call handler validator authorizator id auth))
     (delete-entity [_ id auth]
       (scaffold-delete-call handler validator authorizator id auth))
     (update-entity [_ attributes auth]
       (scaffold-update-call handler validator authorizator attributes auth))
     (add-entity [_ attributes auth]
       (scaffold-add-call handler validator authorizator attributes auth))
     (list-entities [_ criteria sort-attrs from to auth]
       (scaffold-list-call handler validator authorizator criteria sort-attrs from to auth)))))
