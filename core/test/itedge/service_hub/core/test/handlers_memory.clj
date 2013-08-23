(ns itedge.service-hub.core.test.handlers-memory
  (:require [itedge.service-hub.core.handlers-memory :refer :all]
            [itedge.service-hub.core.handlers :refer :all]
            [clojure.test :refer :all]))

(def test-handler (create-memory-handler [{:name "test-entity-one" :thing 2}
                                          {:name "test-entity-two" :thing 7}] :id))

(handle-add-entity test-handler {:name "test-entity-three" :thing 11})
(handle-add-entity test-handler {:name "test-entity-four" :thing 7})

(handle-update-entity test-handler {:id 1 :thing 3})

(deftest test-memory-handler
  (let [entity-one (handle-find-entity test-handler 1)
        entity-two (handle-find-entity test-handler 2)
        entity-three (handle-find-entity test-handler 3)
        q-result-one (handle-list-entities test-handler {:thing 7} nil nil nil)
        q-result-two (handle-list-entities test-handler {:thing 7 :name "test-entity-two"} nil nil nil)
        q-result-three (handle-list-entities test-handler {:name "*-entity-t*"} nil nil nil)
        q-result-four (handle-list-entities test-handler {:name "test-entity-t*"} [[:thing :ASC]] nil nil)
        q-result-five (handle-list-entities test-handler {:thing 7 :name "test-entity-two"} nil nil nil)
        q-result-six (handle-list-entities test-handler nil [[:name :ASC]] 2 4)]
    (is (= (handle-count-entities test-handler nil) 4))
    (is (= (handle-count-entities test-handler {:thing 7}) 2))
    (is (= entity-one {:id 1 :name "test-entity-one" :thing 3}))
    (is (= entity-two {:id 2 :name "test-entity-two" :thing 7}))
    (is (= entity-three {:id 3 :name "test-entity-three" :thing 11}))
    (is (= q-result-one [{:id 2 :name "test-entity-two" :thing 7} {:id 4 :name "test-entity-four" :thing 7}]))
    (is (= q-result-two [{:id 2 :name "test-entity-two" :thing 7}]))
    (is (= q-result-three [{:id 2 :name "test-entity-two" :thing 7} {:id 3 :name "test-entity-three" :thing 11}]))
    (is (= q-result-four [{:id 2 :name "test-entity-two" :thing 7} {:id 3 :name "test-entity-three" :thing 11}]))
    (is (= q-result-five [{:id 2 :name "test-entity-two" :thing 7}]))
    (is (= q-result-six [{:id 3 :name "test-entity-three" :thing 11} {:id 2 :name "test-entity-two" :thing 7}]))
    (is (= (handle-delete-entity test-handler 4) 4))
    (is (= (handle-count-entities test-handler nil) 3))))
