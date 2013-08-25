(ns itedge.service-hub.core.services-util
  (:require [itedge.service-hub.core.util :as util]))

(defn handle-service-exception
  "Handles service exception"
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
