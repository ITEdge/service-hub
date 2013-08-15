(ns itedge.service-hub.http-ring.test.content-util
  (:require [itedge.service-hub.http-ring.content-util :refer :all]
            [clojure.test :refer :all]))

(deftest test-craft-json-response
  (let [{headers :headers body :body} (craft-json-response [{:a 1 :b 2 :ns/c 3} "test" nil])]
    (is (= body "[{\"a\":1,\"b\":2,\"c\":3},\"test\",null]"))
    (is (= (get headers "Content-Type") "application/json"))))

(deftest test-read-json
  (is (= (read-json "[{\"a\":1,\"b\":2},\"test\",null]") [{:a 1 :b 2} "test" nil])))

(deftest test-craft-edn-response
  (let [{headers :headers body :body} (craft-edn-response [{:a 1 :b 2} "test" nil])]
    (is (= body "[{:a 1, :b 2} \"test\" nil]"))
    (is (= (get headers "Content-Type") "application/edn"))))