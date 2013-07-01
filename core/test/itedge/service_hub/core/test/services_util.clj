(ns itedge.service-hub.core.test.services-util
  (:use itedge.service-hub.core.services-util
        clojure.test)
  (:require [itedge.service-hub.core.util :as util]))

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



