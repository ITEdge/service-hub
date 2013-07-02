(ns itedge.service-hub.common.test.util
  (:use itedge.service-hub.common.util
        clojure.test))

(deftest test-update-map-keys-values
  (is (= (update-map-keys-values {1 {:a "t"} 2 {:b "t"}} (fn [k v] (assoc v :k k))) {1 {:k 1 :a "t"} 2 {:k 2 :b "t"}})))

(deftest test-update-if-in
  (is (= (update-if-in {:a 1 :b 2} :c inc) {:a 1 :b 2}))
  (is (= (update-if-in {:a 1 :b 2} :b inc) {:a 1 :b 3})))