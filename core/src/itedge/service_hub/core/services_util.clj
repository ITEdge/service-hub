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
  ([handler id datasource]
     (get-success-response
      (handle-find-entity handler id datasource)))
  ([handler validator id datasource]
     (get-service-result
      (validate-find-entity validator id datasource)
      (scaffold-find-call handler id datasource)))
  ([handler validator authorizator id datasource auth]
     (authorize-service-call
      (authorize-find-call authorizator id datasource auth)
      (scaffold-find-call handler validator id datasource))))

(defn scaffold-delete-call
  "Scaffolds service delete call"
  ([handler id datasource]
     (get-success-delete-response
      (handle-delete-entity handler id datasource)))
  ([handler validator id datasource]
     (get-service-result
      (validate-delete-entity validator id datasource)
      (scaffold-delete-call handler id datasource)))
  ([handler validator authorizator id datasource auth]
     (authorize-service-call
      (authorize-delete-call authorizator id datasource auth)
      (scaffold-delete-call handler validator id datasource))))

(defn scaffold-update-call
  "Scaffolds service update call"
  ([handler attributes datasource]
     (get-success-response
      (handle-update-entity handler attributes datasource)))
  ([handler validator attributes datasource]
     (get-service-result
      (validate-update-entity validator attributes datasource)
      (scaffold-update-call handler attributes datasource)))
  ([handler validator authorizator attributes datasource auth]
     (authorize-service-call
      (authorize-update-call authorizator attributes datasource auth)
      (scaffold-update-call handler validator attributes datasource))))

(defn scaffold-add-call
  "Scaffolds service add call"
  ([handler attributes datasource]
     (get-success-response
      (handle-add-entity handler attributes datasource)))
  ([handler validator attributes datasource]
     (get-service-result
      (validate-add-entity validator attributes datasource)
      (scaffold-add-call handler attributes datasource)))
  ([handler validator authorizator attributes datasource auth]
     (authorize-service-call
      (authorize-add-call authorizator attributes datasource auth)
      (scaffold-add-call handler validator attributes datasource))))

(defn scaffold-list-call
  "Scaffolds service list call"
  ([handler criteria sort-attrs from to datasource]
     (-> (get-success-response 
          (handle-list-entities handler criteria sort-attrs from to datasource))
         (assoc-range-info from to (handle-count-entities handler criteria datasource))))
  ([handler validator criteria sort-attrs from to datasource]
     (get-service-result
      (validate-list-entities validator criteria sort-attrs from to datasource)
      (scaffold-list-call handler criteria sort-attrs from to datasource)))
  ([handler validator authorizator criteria sort-attrs from to datasource auth]
     (authorize-service-call
      (authorize-list-call authorizator criteria datasource auth)
      (let [criteria (restrict-list-call authorizator criteria datasource auth)]
        (scaffold-list-call handler validator criteria sort-attrs from to datasource)))))

(defn scaffold-get-history-call
  "Scaffolds service get-history call"
  ([handler entity-id history-id datasource]
     (get-success-response
      (handle-find-entity-history handler entity-id history-id datasource)))
  ([handler validator entity-id history-id datasource]
     (get-service-result
      (validate-find-entity-history validator entity-id history-id datasource)
      (scaffold-get-history-call handler entity-id history-id datasource)))
  ([handler validator authorizator entity-id history-id datasource auth]
     (authorize-service-call
      (authorize-find-history-call authorizator entity-id history-id datasource auth)
      (scaffold-get-history-call handler validator entity-id history-id datasource))))

(defn scaffold-list-entity-history-call
  "Scaffolds service list-entity-history call"
  ([handler id criteria sort-attrs from to datasource]
     (-> (get-success-response
          (handle-list-entity-history handler id criteria sort-attrs from to datasource))
         (assoc-range-info from to (handle-count-entity-history handler id criteria datasource))))
  ([handler validator id criteria sort-attrs from to datasource]
     (get-service-result
      (validate-list-entity-history validator id criteria sort-attrs from to datasource)
      (scaffold-list-entity-history-call handler id criteria sort-attrs from to datasource)))
  ([handler validator authorizator id criteria sort-attrs from to datasource auth]
     (authorize-service-call
      (authorize-list-history-call authorizator id criteria datasource auth)
      (let [criteria (restrict-list-history-call authorizator id criteria datasource auth)]
        (scaffold-list-entity-history-call handler validator id criteria sort-attrs from to datasource)))))

(defn scaffold-service
  "Scaffolds simple service implementation with mandatory handler and optional
   validator and authorizator arguments"
  ([handler get-datasource-fn]
     (reify PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler id (get-datasource-fn)))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler id (get-datasource-fn)))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler attributes (get-datasource-fn)))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler attributes (get-datasource-fn)))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler criteria sort-attrs from to (get-datasource-fn)))))
  ([handler validator get-datasource-fn]
     (reify PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler validator id (get-datasource-fn)))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler validator id (get-datasource-fn)))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler validator attributes (get-datasource-fn)))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler validator attributes (get-datasource-fn)))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler validator criteria sort-attrs from to (get-datasource-fn)))))
  ([handler validator authorizator get-datasource-fn]
     (reify PEntityService
       (find-entity [_ id auth]
         (scaffold-find-call handler validator authorizator id (get-datasource-fn) auth))
       (delete-entity [_ id auth]
         (scaffold-delete-call handler validator authorizator id (get-datasource-fn) auth))
       (update-entity [_ attributes auth]
         (scaffold-update-call handler validator authorizator attributes (get-datasource-fn) auth))
       (add-entity [_ attributes auth]
         (scaffold-add-call handler validator authorizator attributes (get-datasource-fn) auth))
       (list-entities [_ criteria sort-attrs from to auth]
         (scaffold-list-call handler validator authorizator criteria sort-attrs from to (get-datasource-fn) auth)))))
