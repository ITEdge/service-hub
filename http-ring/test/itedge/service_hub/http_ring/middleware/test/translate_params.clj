(ns itedge.service-hub.http-ring.middleware.test.translate-params
  (:use itedge.service-hub.http-ring.middleware.translate-params
        clojure.test))

(deftest test-wrap-translate-params
  (let [wrapped-handler (wrap-translate-params identity)
        request {:params {:a "null" :b "true" :c "false" :d "[1,2,3]"}}
        translated-params (:params (wrapped-handler request))]
    (is (= (:a translated-params) nil))
    (is (= (:b translated-params) true))
    (is (= (:c translated-params) false))
    (is (= (:d translated-params) [1 2 3]))))