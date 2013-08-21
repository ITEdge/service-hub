(ns itedge.service-hub.core.test.util
  (:require [itedge.service-hub.core.util :refer :all]
            [clojure.test :refer :all]))

(deftest test-in?
  (is (= (in? [1 2 3 4 5] 2) true))
  (is (= (in? [1 2 3 4 5] 7) nil)))

(deftest test-wildcard-compare
  (is (= (wildcard-compare 1 1) true))
  (is (= (wildcard-compare 1 0) false))
  (is (= (wildcard-compare "test" "est") false))
  (is (= (wildcard-compare "test" "*est") true))
  (is (= (wildcard-compare "test" "*es*") true))
  (is (= (wildcard-compare "test" "te*") true)))

(deftest test-get-ranged-vector
  (is (= (get-ranged-vector '(0 1 2 3 4 5) nil nil) [0 1 2 3 4 5]))
  (is (= (get-ranged-vector '(0 1 2 3 4 5) 2 nil) [2 3 4 5]))
  (is (= (get-ranged-vector '(0 1 2 3 4 5) nil 2) [0 1]))
  (is (= (get-ranged-vector '(0 1 2 3 4 5) 2 5) [2 3 4])))

(deftest test-sort-maps
  (is (= (sort-maps '({:a 1 :b 2} {:a 7 :b 5} {:a 3 :b 2} {:a 7 :b 4}) [[:a :ASC] [:b :ASC]]) 
         [{:a 1 :b 2} {:a 3 :b 2} {:a 7 :b 4} {:a 7 :b 5}])))

(deftest test-update-map-values
  (is (= (update-map-values {:a 1 :b 2} inc) {:a 2 :b 3})))

(deftest test-parse-number
  (is (= (parse-number "0.1") 0.1))
  (is (= (parse-number "1") 1))
  (is (= (parse-number "1a") nil)))

(deftest test-abs
  (is (= (abs -1) 1))
  (is (= (abs 0) 0))
  (is (= (abs 1) 1)))

(deftest test-get-service-result
  (is (= (get-service-result 1 "test result") {:return-code 1 :message "test result"})))

(deftest test-pipeline-statements
  (is (= (pipeline-statements ((fn [] nil)) ((fn [] :ok))) :ok))
  (is (= (pipeline-statements ((fn [] nil)) ((fn [] nil))) nil))
  (is (= (pipeline-statements nil) nil)))



