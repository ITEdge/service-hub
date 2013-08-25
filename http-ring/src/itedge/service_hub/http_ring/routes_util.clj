(ns itedge.service-hub.http-ring.routes-util
  (:require [itedge.service-hub.core.services :refer :all]
            [ring.util.request :as request-util]
            [compojure.core :refer :all]
            [clojure.string :as string]
            [itedge.service-hub.core.util :as util]))

(defn- parse-sort-args
  "Parses sort parameters from request in form 'sortParam=+param1,-param2' and
   transforms them into arguments vector of form for example [[:param1 :ASC] [:param2 :DESC]]"
  [sort-key params]
  (when-let [params (sort-key params)] 
    (mapv #(vector (keyword (.replaceAll % "\\s|\\+|-" "")) ({\space :ASC \+ :ASC \- :DESC} (first %))) 
          (string/split params #","))))

(defn- parse-range-headers
  "Parses range headers, returns map with keys :from and :to, for example {:from 0 :to 24}, if no range headers
   are present, returns nil"
  [{headers :headers }]
  (when-let [range-headers (get headers "range")]
    (let [chunks (string/split (last (string/split range-headers #"=")) #"-")]
      {:from (util/parse-number (first chunks)) :to (util/parse-number (last chunks))})))

(defn- create-content-range-headers
  "If all arguments are present, creates content range header and adds it to the response"
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
    [response {return-code :return-code}]
      (assoc response :status (return-code results-to-codes))))

(defn list-route
  "Returns route for list calls - 'GET path/' (uses sorting parameters from request if present).
   Takes the root path, listing function, one argument post-processiong function and one 
   argument content-write function as arguments. Listing function takes four optional arguments,
   sorting vector, lower range bound, upper range bound and authentication map."
  [path list-fn post-processing-fn content-write]
  (GET (str path "/") [:as request]
       (let [range (parse-range-headers request)
             sort-args (parse-sort-args :sortBy (:params request))
             auth (:auth request)
             result (list-fn sort-args (:from range) (:to range) auth)]
         (-> (content-write 
           (-> (:message result)
             (post-processing-fn)))
             (create-status-code result)
             (create-content-range-headers result)))))

(defn get-route
  "Returns route for get calls - 'GET path/:id'. Takes the root path, get function, 
   one argument post-processiong function and one argument content-write function as arguments. 
   Get function takes two arguments, entity id and optional authentication map."
  [path get-fn post-processing-fn content-write]
  (GET [(str path "/:id"), :id #"[0-9]+"] [id :as request]
       (let [auth (:auth request)
             result (get-fn (util/parse-number id) auth)]
         (-> (content-write 
           (-> (:message result)
             (post-processing-fn)))
             (create-status-code result)))))

(defn query-route
  "Returns route for query calls - 'GET path' (uses querying and sorting parameters from request if present).
   Takes the root path, querying function, one argument post-processiong function and one 
   argument content-write function as arguments. Querying function takes five optional arguments,
   criteria query map, sorting vector, lower range bound, upper range bound and authentication map."
  [path query-fn post-processing-fn content-write]
  (GET path [:as request]
       (let [range (parse-range-headers request)
             params (:params request)
             sort-args (parse-sort-args :sortBy params)
             auth (:auth request)
             result (query-fn params sort-args (:from range) (:to range) auth)]
         (-> (content-write
           (-> (:message result)
             (post-processing-fn)))
           (create-status-code result)
           (create-content-range-headers result)))))

(defn update-route
  "Returns route for update calls - 'PUT path/:id'. Takes the root path, update function, 
   one argument post-processiong function, entity primary key, one argument content-read function and
   one argument content-write function as arguments. Update function takes two arguments, entity attributes
   and optional authentication map."
  [path update-fn post-processing-fn pk content-read content-write]
  (PUT [(str path "/:id"), :id #"[0-9]+"] [id :as request]
       (let [attributes (assoc (content-read (request-util/body-string request)) pk (util/parse-number id))
             auth (:auth request)
             result (update-fn attributes auth)]
         (-> (content-write
           (-> (:message result)
             (post-processing-fn)))
           (create-status-code result)))))

(defn create-route
  "Returns route for create calls - 'POST path/'. Takes the root path, create function, one argument 
   post-processiong function, one argument content-read function and one argument content-write function as 
   arguments. Create function takes two arguments, entity attributes and optional authentication map."
  [path create-fn post-processing-fn content-read content-write]
  (POST path [:as request]
        (let [attributes (content-read (request-util/body-string request))
              auth (:auth request)
              result (create-fn attributes auth)] 
	  (-> (content-write
            (-> (:message result)
              (post-processing-fn)))
            (create-status-code result)))))

(defn delete-route
  "Returns route for delete calls - 'DELETE path/:id'. Takes the root path, delete function, one argument 
   post-processiong function and one argument content-write function as arguments. Delete function takes 
   two arguments, entity id and optional authentication map."
  [path delete-fn post-processing-fn content-write]
  (DELETE [(str path "/:id"), :id #"[0-9]+"] [id :as request]
          (let [auth (:auth request)
                result (delete-fn (util/parse-number id) auth)]
	    (-> (content-write
              (-> (:message result)
                (post-processing-fn)))
              (create-status-code result)))))

(defn scaffold-crud-routes
  "Creates standard REST routes for all CRUD operations, taking root path, entity service
   content-read, content-write functions and entity primary key as attributes. 
   Created paths for according http verbs are: 
   1. List   - 'GET path/' (uses sorting parameters from request if present)
   2. Get    - 'GET path/:id'
   3. Query  - 'GET path' (uses querying and sorting parameters from request if present)
   4. Update - 'PUT path/:id'
   5. Create - 'POST path/'
   6. Delete - 'DELETE path/:id'
   Map with post functions for each path (:list :get :query :update :create :delete) can be 
   optionally passed in as fourth argument, corresponding functions will be then called with
   result of the entity service call"
  ([path entity-service pk content-read content-write]
    (scaffold-crud-routes path entity-service pk content-read content-write nil))
  ([path entity-service pk content-read content-write post-fns]
    (routes 
      (list-route path (partial list-entities entity-service nil) (:list post-fns identity) content-write)
      (get-route path (partial find-entity entity-service) (:get post-fns identity) content-write)
      (query-route path (partial list-entities entity-service) (:query post-fns identity) content-write)
      (update-route path (partial update-entity entity-service) (:update post-fns identity) pk content-read content-write)     
      (create-route path (partial add-entity entity-service) (:create post-fns identity) content-read content-write)
      (delete-route path (partial delete-entity entity-service) (:delete post-fns identity) content-write))))

(defn deny-request 
  "Deny request with specified reason"
  [reason content-write]
  (-> (content-write reason)
    (assoc :status 401)))

(defn check-auth
  "Checks authentication info, if it's map, authentication is valid and authentication map is returned as response,
   deny otherwise"
  {:added "EBS 1.0"}
  [{auth :auth} content-write]
  (if (map? auth)
    (content-write auth)
    (deny-request auth content-write)))
