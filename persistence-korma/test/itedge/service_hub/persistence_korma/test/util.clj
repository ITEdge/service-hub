(ns itedge.service-hub.persistence-korma.test.util
  (:use itedge.service-hub.persistence-korma.util
        clojure.test))

(deftest test-translate-relation-expressions
  (is (= (translate-relation-expressions {:a 1 :b {:not 2} :c {:not-in [3 4]} :d {:in [5 6]} 
                                          :e {:gt 7} :f {:lt 8} :g {:gteq 9} :h {:lteq 10}}) nil)))