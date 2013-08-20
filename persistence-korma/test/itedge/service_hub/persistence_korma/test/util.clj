(ns itedge.service-hub.persistence-korma.test.util
  (:require [itedge.service-hub.persistence-korma.util :refer :all]
            [clojure.test :refer :all]
            [korma.sql.fns :as korma-fns]))

(deftest test-translate-relation-expressions
  (is (= (translate-relation-expressions {:a 1 :b {:not 2} :c {:not-in [3 4]} :d {:in [5 6]} 
                                          :e {:gt 7} :f {:lt 8} :g {:gteq 9} :h {:lteq 10}})
         {:a 1 :b [korma-fns/pred-not= 2] :c [korma-fns/pred-not-in [3 4]] :d [korma-fns/pred-in [5 6]]
          :e [korma-fns/pred-> 7] :f [korma-fns/pred-< 8] :g [korma-fns/pred->= 9] :h [korma-fns/pred-<= 10]})))