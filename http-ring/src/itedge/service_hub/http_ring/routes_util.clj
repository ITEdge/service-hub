(ns itedge.service-hub.http-ring.routes-util
  (:use itedge.service-hub.core.services
        compojure.core)
  (:require [clojure.string :as string]
            [itedge.service-hub.core :as util]
            [itedge.service-hub.http-ring.content-util :as content-util]))

(defn- parse-sort-args
  "Parses sort parameters from request in form 'sortParam=+param1,-param2' and
   transforms them into arguments vector of form for example [[:param1 :ASC] [:param2 :DESC]]"
  {:added "EBS 1.0"}
  [sort-key params]
  (when-let [params (sort-key params)]
    (into [] 
      (map #(vector (keyword (.replaceAll % "\\s|\\+|-" "")) ({\space :ASC \+ :ASC \- :DESC} (first %))) 
           (string/split params #",")))))

(defn- parse-range-headers
  "Parses range headers, returns map with keys :from and :to, for example {:from 0 :to 24}, if no range headers
   are present, returns nil"
  {:added "EBS 1.0"}
  [{headers :headers }]
  (when-let [range-headers (get headers "range")]
    (let [chunks (string/split (last (string/split range-headers #"=")) #"-")]
      {:from (util/parse-number (first chunks)) :to (util/parse-number (last chunks))})))

(defn- create-content-range-headers
  "If all arguments are present, creates content range header and adds it to the response"
  {:added "EBS 1.0"}
  [response {:keys [from to total]}]
  (if (and from to total)
    (assoc-in response [:headers "Content-Range"] (str "items " from "-" to "/" total))
    response))

; map service-results -> http codes
(let [results-to-codes {:ok 200 :delete-ok 204 
                        :not-authenticated 403 :not-authorized 403 :not-found 404 :conflict 409 :gone 410 :bad-range 416 
                        :error 500}]
  (defn create-status-code
	  "Translate status from service result to valid http status code"
	  {:added "EBS 1.0"}
		  [response {return-code :return-code}]
		  (assoc response :status (return-code results-to-codes))))

(defn scaffold-crud-routes
  "Creates standard REST routes for all CRUD operations, taking root path, entity service
   and entity primary key as attributes. Created paths for according http verbs are: 
   1. List   - 'GET path/' (uses sorting parameters from request if present)
   2. Get    - 'GET path/:id'
   3. Query  - 'GET path' (uses querying and sorting parameters from request if present)
   4. Update - 'PUT path/:id'
   5. Create - 'POST path/'
   6. Delete - 'DELETE path/:id'
   Map with post functions for each path (:list :get :query :update :create :delete) can be 
   optionally passed in as fourth argument, corresponding functions will be then called with
   result of entity service call"
  {:added "EBS 1.0"}
  ([path entity-service pk]
    (scaffold-crud-routes path entity-service pk nil))
  ([path entity-service pk post-fns]
   (routes (GET (str path "/") [:as request]
               (let [range (parse-range-headers request)
                     sort-args (parse-sort-args :sortBy (:params request))
                     auth (:auth request)
                     result (list-entities entity-service nil sort-args (:from range) (:to range) auth)]
	               (-> (content-util/craft-json-response 
                   (-> (:message result)
	                     ((:list post-fns identity))))
                   (create-status-code result)
                   (create-content-range-headers result))))
           (GET [(str path "/:id"), :id #"[0-9]+"] [id :as request]
               (let [auth (:auth request)
                     result (find-entity entity-service id auth)]
	               (-> (content-util/craft-json-response 
                   (-> (:message result)
                       ((:get post-fns identity))))
                   (create-status-code result))))
           (GET path [:as request]
               (let [range (parse-range-headers request)
                     params (:params request)
                     sort-args (parse-sort-args :sortBy params)
                     auth (:auth request)
                     result (list-entities entity-service params sort-args (:from range) (:to range) auth)]
                 (-> (content-util/craft-json-response
                   (-> (:message result)
                       ((:query post-fns identity))))
                   (create-status-code result)
                   (create-content-range-headers result))))
           (PUT [(str path "/:id"), :id #"[0-9]+"] [id :as request]
               (let [attributes (assoc (util/read-json (slurp (:body request))) pk id)
                     auth (:auth request)
                     result (update-entity entity-service attributes auth)]
	               (-> (content-util/craft-json-response
                   (-> (:message result)
                       ((:update post-fns identity))))
                   (create-status-code result))))
           (POST path [:as request]
               (let [attributes (util/read-json (slurp (:body request)))
                     auth (:auth request)
                     result (add-entity entity-service attributes auth)] 
	               (-> (content-util/craft-json-response
                   (-> (:message result)
                       ((:create post-fns identity))))
                   (create-status-code result))))
           (DELETE [(str path "/:id"), :id #"[0-9]+"] [id :as request]
               (let [auth (:auth request)
                     result (delete-entity entity-service id auth)]   
	               (-> (content-util/craft-json-response
                   (-> (:message result)
                       ((:delete post-fns identity))))
                   (create-status-code result)))))))

(defn deny-request 
  "Deny request with specified reason"
  [reason]
  (-> (content-util/craft-json-response reason)
    (assoc :status 401)))

(defn check-auth
  "Checks authentication info, if it's map, authentication is valid and authentication map is returned as response, 
   deny otherwise"
  {:added "EBS 1.0"}
  [{auth :auth}]
  (if (map? auth)
    (content-util/craft-json-response auth)
    (deny-request auth)))