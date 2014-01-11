(ns itedge.service-hub.http-ring.test.routes-util
  (:require [itedge.service-hub.core.handlers :refer :all]
            [itedge.service-hub.core.services :refer :all]
            [itedge.service-hub.http-ring.routes-util :refer :all]
            [compojure.core :refer :all]
            [clojure.test :refer :all]
            [itedge.service-hub.core.handlers-memory :as handlers-memory]
            [itedge.service-hub.core.services-util :as services-util]
            [itedge.service-hub.http-ring.content-util :as content-util]))

(def test-handler (handlers-memory/create-handler [{:name "test-entity-one" :thing 2}
                                                   {:name "test-entity-two" :thing 7}
                                                   {:name "test-entity-three" :thing 9}
                                                   {:name "test-entity-four" :thing 11}] :id))

(deftype TestService [test-handler]
  PEntityService
  (find-entity [_ id auth]
    (services-util/get-service-result
     (services-util/get-success-response
      (handle-find-entity test-handler id nil))))
  (delete-entity [_ id auth]
    (services-util/get-service-result
     (services-util/get-success-delete-response 
      (handle-delete-entity test-handler id nil))))
  (update-entity [_ attributes auth]
    (services-util/get-service-result
     (services-util/get-success-response 
      (handle-update-entity test-handler attributes nil))))
  (add-entity [_ attributes auth]
    (services-util/get-service-result
     (services-util/get-success-response 
      (handle-add-entity test-handler attributes nil))))
  (list-entities [_ criteria sort-attrs from to auth]
    (services-util/get-service-result
     (-> (services-util/get-success-response 
          (handle-list-entities test-handler criteria sort-attrs from to nil))
         (services-util/assoc-range-info from to (handle-count-entities test-handler criteria nil))))))

(def test-service (->TestService test-handler))

(defroutes test-routes
  (scaffold-crud-routes "/tests" test-service :id content-util/read-json content-util/craft-json-response true))

(deftest test-scaffolded-routes
  (is (= (test-routes {:request-method :get :uri "/tests/1"}) 
         {:status 200 :headers {"Content-Type" "application/json"} 
          :body "{\"name\":\"test-entity-one\",\"thing\":2,\"id\":1}"}))
  (is (= (test-routes {:request-method :get :uri "/testss/1"}) nil))
  (is (= (test-routes {:request-method :post :uri "/tests" :body "{\"name\":\"test-entity-five\",\"thing\":11}"})
         {:status 200 :headers {"Content-Type" "application/json"} 
          :body "{\"id\":5,\"name\":\"test-entity-five\",\"thing\":11}"}))
  (is (= (:headers (test-routes {:request-method :get :uri "/tests/"}))
         {"Content-Type" "application/json"
          "Content-Range" "items 0-4/5"}))
  (is (= (test-routes {:request-method :get :uri "/tests" :params {:name "test-entity-five"}})
         {:status 200 :headers {"Content-Type" "application/json" "Content-Range" "items 0-0/1"}
          :body "[{\"id\":5,\"name\":\"test-entity-five\",\"thing\":11}]"}))
  (is (= (test-routes {:request-method :put :uri "/tests/1" :body "{\"thing\":1}"})
         {:status 200 :headers {"Content-Type" "application/json"} 
          :body "{\"name\":\"test-entity-one\",\"thing\":1,\"id\":1}"}))
  (is (= (test-routes {:request-method :delete :uri "/tests/5"})
         {:status 204 :headers {"Content-Type" "application/json"} 
          :body "5"}))
  (is (= (:headers (test-routes {:request-method :get :uri "/tests/"}))
         {"Content-Type" "application/json"
          "Content-Range" "items 0-3/4"})))

(let [data [{:id 1 :t 1} {:id 1 :t 2} {:id 1 :t 3} {:id 2 :t 1}]]
  (defn fake-list-history-fn [id criteria sort-attrs from to auth]
    (let [filtered (into [] (filter (fn [item] (= (:id item) id)) data))]
      (-> (services-util/get-success-response filtered)
          (services-util/assoc-range-info from to (count filtered)))))

  (defn fake-get-history-fn [entity-id history-id auth]
    (services-util/get-success-response 
     (first (filter (fn [item] 
                      (and (= (:id item) entity-id) 
                           (= (:t item) history-id))) data)))))

(def test-history-service 
  (reify PEntityHistoryService
    (find-entity-history [_ entity-id history-id auth]
      (fake-get-history-fn entity-id history-id auth))
    (list-entity-history [_ id criteria sort-attrs from to auth]
      (fake-list-history-fn id criteria sort-attrs from to auth))))

(def history-routes
  (apply routes (get-crud-history-routes "/tests" test-history-service content-util/craft-edn-response true)))

(deftest test-history-routes
  (is (= (:headers (history-routes {:request-method :get :uri "/tests/1/history/"}))
         {"Content-Type" "application/edn"
          "Content-Range" "items 0-2/3"}))
  (is (= (:headers (history-routes {:request-method :get :uri "/tests/1/history"}))
         {"Content-Type" "application/edn"
          "Content-Range" "items 0-2/3"}))
  (is (= (history-routes {:request-method :get :uri "/tests/1/history/2"})
         {:status 200 :headers {"Content-Type" "application/edn"} 
          :body "{:t 2, :id 1}"})))

(deftest test-deny-request
  (is (= (deny-request "test reason" content-util/craft-json-response)
         {:status 401 :headers {"Content-Type" "application/json"} :body "test reason"})))

(deftest test-check-auth
  (is (= (check-auth {:username "test" :password "test"} content-util/craft-json-response)
         {:status 200 :headers {"Content-Type" "application/json"} 
          :body "{\"username\":\"test\",\"password\":\"test\"}"}))
  (is (= (check-auth "bad credentials" content-util/craft-json-response)
         (deny-request "bad credentials" content-util/craft-json-response))))



