(ns itedge.service-hub.core.test.services-util
  (:require [itedge.service-hub.core.services-util :refer :all]
            [itedge.service-hub.core.handlers :refer :all]
            [itedge.service-hub.core.services :refer :all]
            [itedge.service-hub.core.validators :refer :all]
            [itedge.service-hub.core.authorizators :refer :all]
            [clojure.test :refer :all]
            [itedge.service-hub.core.handlers-memory :as handlers-memory]
            [itedge.service-hub.core.util :as util]))

(deftest test-handle-service-exception
  (let [result (handle-service-exception (Exception. "test"))]
    (is (= (:return-code result) :error))))

(deftest test-get-service-result
  (let [service-fn (fn [s] (reduce + s))
        validator-fn-1 (fn [s] (if (empty? s) 
                                 (util/get-service-result :conflict "Source is empty")
                                 nil))
        validator-fn-2 (fn [s] (if (= (first s) 1)
                                 (util/get-service-result :conflict "First element of source is one")
                                 nil))
        source [1 2 3]]
    (is (= (get-service-result (service-fn source)) 6))
    (is (= (get-service-result (validator-fn-1 source) (service-fn source)) 6))
    (is (= (get-service-result (validator-fn-2 source) (service-fn source)) 
           (util/get-service-result :conflict "First element of source is one")))))

(deftest test-authorize-service-call
  (let [service-fn (fn [s] (reduce + s))
        authorizator-fn-1 (fn [{roles :roles}]
                            (if (util/in? roles :user)
                              nil
                              (util/get-service-result :not-authorized "not authorized")))
        authorizator-fn-2 (fn [{roles :roles}]
                            (if (util/in? roles :admin)
                              nil
                              (util/get-service-result :not-authorized "not authorized")))
        source [1 2 3]
        auth {:username "test" :roles #{:user}}]
    (is (= (authorize-service-call (authorizator-fn-1 auth) (service-fn source)) 6))
    (is (= (authorize-service-call (authorizator-fn-2 auth) (service-fn source))
           (util/get-service-result :not-authorized "not authorized")))))

(deftest test-do-side-effects
  (let [state-atom (atom 0)
        pure-fn (fn [] :ok)]
    (is (= (do-side-effects (pure-fn) (swap! state-atom inc)) :ok))
    (is (= @state-atom 1))))

(deftest test-get-success-response
  (is (= (get-success-response "Operation succesful")
         (util/get-service-result :ok "Operation succesful"))))

(deftest test-get-success-delete-response
  (is (= (get-success-delete-response "Operation succesful")
         (util/get-service-result :delete-ok "Operation succesful"))))

(deftest test-assoc-range-info
  (let [response (assoc-range-info (get-success-response [10 11 12 13 14]) 10 15 30)]
    (is (= (:from response) 10))
    (is (= (:to response) 15))
    (is (= (:total response) 30)))
  (let [response (assoc-range-info (get-success-response [1 2 3]) nil nil 3)]
    (is (= (:from response) 0))
    (is (= (:to response) 2))
    (is (= (:total response) 3)))
  (let [response (assoc-range-info (get-success-response [20 21 22]) 20 29 23)]
    (is (= (:from response) 20))
    (is (= (:to response) 22))
    (is (= (:total response) 23))))

(deftest test-scaffold-service
  (let [test-handler (handlers-memory/create-handler [{:name "test 1"} {:name "test 2"}] :id)
        test-validator (reify PEntityServiceValidator
                         (validate-find-entity [_ id _] nil)
                         (validate-delete-entity [_ id _] nil)
                         (validate-update-entity [_ attributes _] nil)
                         (validate-add-entity [_ attributes _] nil)
                         (validate-list-entities [_ criteria sort-attrs from to _] nil))
        test-authorizator (reify PEntityServiceAuthorizator
                            (authorize-find-call [_ id _ auth] nil)
                            (authorize-delete-call [_ id _ auth] nil)
                            (authorize-update-call [_ attributes _ auth] nil)
                            (authorize-add-call [_ attributes _ auth] nil)
                            (authorize-list-call [_ criteria _ auth] nil)
                            (restrict-list-call [_ criteria _ auth] criteria))
        test-service (scaffold-service test-handler test-validator test-authorizator nil (fn [] nil))]
    (is (= (get-success-response {:id 1 :name "test 1"}) (find-entity test-service 1 nil)))
    (is (= (get-success-delete-response 2) (delete-entity test-service 2 nil)))
    (is (= (get-success-response {:id 1 :name "updated 1"}) (update-entity test-service {:id 1 :name "updated 1"} nil)))
    (is (= (get-success-response {:id 3 :name "test 3"}) (add-entity test-service {:name "test 3"} nil)))
    (is (= (-> (get-success-response [{:id 1 :name "updated 1"} {:id 3 :name "test 3"}])
               (assoc-range-info nil nil (handle-count-entities test-handler nil nil))) (list-entities test-service nil nil nil nil nil)))))
