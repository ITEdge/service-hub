(ns itedge.service-hub.core.test.convertors-util
  (:require [itedge.service-hub.core.convertors-util :refer :all]
            [clojure.test :refer :all]))

(deftest test-convert-specified-values
  (is (= (convert-specified-values nil #{:a :b} inc) nil))
  (is (= (convert-specified-values {:a 1 :b 2 :c 3} #{:a :b} inc) {:a 2 :b 3 :c 3}))
  (is (= (convert-specified-values [{:a 1 :b 2} {:a 1 :b 2 :c 3} {:a 1}] #{:a :b} inc)
         [{:a 2 :b 3} {:a 2 :b 3 :c 3} {:a 2}])))

(deftest test-format-property
  (let [format-fn (format-property "%.2f")]
    (is (= (format-fn nil) nil))
    (is (= (format-fn 0.10123) "0.10"))))

(deftest test-sanitize-iso-params
  (is (= (sanitize-iso-params identity "1997-07-16T19:20:30+01:00") "1997-07-16T19:20:30+01:00"))
  (is (= (sanitize-iso-params identity "1997-07-16T19:20:30 01:00") "1997-07-16T19:20:30+01:00")))