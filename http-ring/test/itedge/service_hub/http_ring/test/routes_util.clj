(ns itedge.service-hub.http-ring.test.routes-util
  (:use itedge.service-hub.core.handlers
        itedge.service-hub.core.services
        itedge.service-hub.http-ring.routes-util
        itedge.service-hub.http-ring.middleware.translate-params
        compojure.core
        [ring.middleware params
                         keyword-params
                         nested-params]
        clojure.test)
  (:require [itedge.service-hub.core.handlers-memory :as handlers-memory]
            [itedge.service-hub.core.services-util :as services-util]))

(def test-handler (handlers-memory/create-memory-handler [{:name "test-entity-one" :thing 2}
                                                          {:name "test-entity-two" :thing 7}
                                                          {:name "test-entity-three" :thing 9}
                                                          {:name "test-entity-four" :thing 11}] :id))

(deftype TestService [test-handler]
  PEntityService
  (find-entity [_ id auth]
    (services-util/get-service-result
	    (services-util/get-success-response
        (handle-find-entity test-handler id))))
  (delete-entity [_ id auth]
    (services-util/get-service-result
      (services-util/get-success-delete-response 
        (handle-delete-entity test-handler id))))
  (update-entity [_ attributes auth]
    (services-util/get-service-result
      (services-util/get-success-response 
        (handle-update-entity test-handler attributes))))
  (add-entity [_ attributes auth]
    (services-util/get-service-result
      (services-util/get-success-response 
        (handle-add-entity test-handler attributes))))
  (list-entities [_ criteria sort-attrs from to auth]
    (services-util/get-service-result
	    (-> (services-util/get-success-response 
            (handle-list-entities test-handler criteria sort-attrs from to))
        (services-util/assoc-range-info from to (handle-count-entities test-handler criteria))))))

(def test-service (->TestService test-handler))

(defroutes test-routes
  (scaffold-crud-routes "/tests" test-service :id))

(def test-app
  (-> test-routes
    (wrap-translate-params)
    (wrap-keyword-params)
    (wrap-nested-params)
    (wrap-params)))

(deftest test-scaffolded-routes
  (let [response (test-app {:request-method :get :uri "/tests/1"})]
    (is (= response {:status 200 
                     :headers {"Content-Type" "application/json"}
                     :body "{\"name\":\"test-entity-one\",\"thing\":2,\"id\":1}"})))
  (let [response (test-app {:request-method :get :uri "/testss/1"})]
    (is (= response nil))))



