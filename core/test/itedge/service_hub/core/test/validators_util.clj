(ns itedge.service-hub.core.test.validators-util
  (:require [itedge.service-hub.core.validators-util :refer :all]
            [itedge.service-hub.core.handlers :refer :all]
            [clojure.test :refer :all]
            [itedge.service-hub.core.handlers-memory :as handlers-memory]
            [itedge.service-hub.core.util :as util]
            [itedge.service-hub.core.date-time-util :as date-time-util]))

(def test-entity-handler 
  (handlers-memory/create-memory-handler [{:name "test-entity-one" :thing 2 :linked nil :things [2 3]
                                           :updated (date-time-util/iso-8061-to-datetime "2013-07-01T22:15:00.000+02:00")}
                                          {:name "test-entity-two" :thing 5 :linked 1 :things [1]
                                           :updated (date-time-util/iso-8061-to-datetime "2013-07-01T22:15:00.000+02:00")}
                                          {:name "test-entity-three" :thing 7 :linked nil 
                                           :updated (date-time-util/iso-8061-to-datetime "2013-07-01T22:15:00.000+02:00")}] :id))

(deftest test-validate-insert-fields
  (is (= (validate-insert-fields {:a 1 :b 2 :c 3} #{:a :b}) nil))
  (is (= (validate-insert-fields {:a 1 :b nil :c 3} #{:a :b})
         (util/get-service-result :conflict "one or more mandatory fields are missing or empty")))
  (is (= (validate-insert-fields {:a 1} #{:a :b})
         (util/get-service-result :conflict "one or more mandatory fields are missing or empty"))))

(deftest test-validate-update-fields
  (is (= (validate-update-fields {:a 1 :b 2 :c 3} #{:a :b}) nil))
  (is (= (validate-update-fields {:a 1 :b nil :c 3} #{:a :b})
         (util/get-service-result :conflict "one or more mandatory fields have null values")))
  (is (= (validate-update-fields {:a 1} #{:a :b}) nil)))

(deftest test-validate-insert-update-relations
  (is (= (validate-insert-update-relations {:item 1} :item test-entity-handler) nil))
  (is (= (validate-insert-update-relations {:item 4} :thing test-entity-handler) nil))
  (is (= (validate-insert-update-relations {:item 4} :item test-entity-handler)
         (util/get-service-result :conflict "mandatory related entity with given primary key does not exists"))))

(deftest test-validate-mandatory-insert-relations
  (is (= (validate-mandatory-insert-relations {:item 1} :item test-entity-handler) nil))
  (is (= (validate-mandatory-insert-relations {:item 4} :thing test-entity-handler)
         (util/get-service-result :conflict "mandatory related entity not present")))
  (is (= (validate-mandatory-insert-relations {:item 4} :item test-entity-handler)
         (util/get-service-result :conflict "mandatory related entity with given primary key does not exists"))))

(deftest test-validate-unique-fields
  (is (= (validate-unique-fields {:id 1 :name "test-entity-one"} test-entity-handler #{:name}) nil))
  (is (= (validate-unique-fields {:id 2 :name "test-entity-one"} test-entity-handler #{:name})
         (util/get-service-result :conflict "one or more unique values in entity are not unique among other entities"))))

(deftest test-validate-entity-still-there
  (is (= (validate-entity-still-there {:id 1 :name "changed"} test-entity-handler) nil))
  (is (= (validate-entity-still-there {:name "changed"} test-entity-handler)
         (util/get-service-result :conflict "requested entity does not posses id, which is required to check it's availability")))
  (is (= (validate-entity-still-there {:id 4 :name "changed"} test-entity-handler)
         (util/get-service-result :gone "requested entity does not exist anymore, it was probably deleted by another user"))))

(deftest test-validate-entity-present
  (is (= (validate-entity-present 1 test-entity-handler) nil))
  (is (= (validate-entity-present 4 test-entity-handler) 
         (util/get-service-result :not-found "entity with requested id was not found"))))

(deftest test-validate-delete-relations
  (is (= (validate-delete-relations 1 :thing test-entity-handler) nil))
  (is (= (validate-delete-relations 4 :things test-entity-handler) nil))
  (is (= (validate-delete-relations 7 :thing test-entity-handler) 
         (util/get-service-result :conflict "entity requested for deletion has mandatory relations to other entities")))
  (is (= (validate-delete-relations 1 :things test-entity-handler)
         (util/get-service-result :conflict "entity requested for deletion has mandatory relations to other entities"))))

(deftest test-validate-list-range
  (is (= (validate-list-range 0 5 nil test-entity-handler) nil))
  (is (= (validate-list-range 0 5 {:name "test-entity-one"} test-entity-handler) nil))
  (is (= (validate-list-range 10 15 nil test-entity-handler) 
         (util/get-service-result :bad-range "Total count of given entities in system is smaller than lower range restriction")))
  (is (= (validate-list-range 10 5 nil test-entity-handler)
         (util/get-service-result :conflict "Wrong range selection, 'from' must be smaller then 'to' boundary"))))

(deftest test-validate-implication-insert
  (is (= (validate-implication-insert {:a 1 :b 2} :a :b) nil))
  (is (= (validate-implication-insert {:a 1 :b nil} :a :b)
         (util/get-service-result :conflict "Implicative properties relation violated")))
  (is (= (validate-implication-insert {:a 1} :a :b)
         (util/get-service-result :conflict "Implicative properties relation violated"))))

(deftest test-validate-implication-update
  (is (= (validate-implication-update {:id 1 :thing 4 :name "changed"} test-entity-handler :thing :name) nil))
  (is (= (validate-implication-update {:id 1 :thing 9} test-entity-handler :thing :name) nil))
  (is (= (validate-implication-update {:id 1 :thing 11} test-entity-handler :thing :linked)
         (util/get-service-result :conflict "Implicative properties relation violated")))
  (is (= (validate-implication-update {:id 4 :thing 12} test-entity-handler :thing :linked)
         (util/get-service-result :conflict "Implicative properties relation violated"))))

(deftest test-validate-date-times-chronology
  (is (= (validate-date-times-chronology {:first "2013-06-28T22:15:00.000+02:00" 
                                          :second "2013-07-01T22:15:00.000+02:00"} test-entity-handler :first :second) nil))
  (is (= (validate-date-times-chronology {:id 1 :created "2013-06-28T22:15:00.000+02:00"} 
                                         test-entity-handler :created :updated) nil))
  (is (= (validate-date-times-chronology {:first "2013-07-28T22:15:00.000+02:00" 
                                          :second "2013-07-01T22:15:00.000+02:00"} test-entity-handler :first :second)
         (util/get-service-result :conflict "there is conflict in date-times chronology"))) 
  (is (= (validate-date-times-chronology {:id 1 :created "2013-07-28T22:15:00.000+02:00"} 
                                         test-entity-handler :created :updated)
         (util/get-service-result :conflict "there is conflict in date-times chronology"))))

(deftest test-validate-iso-date-times
  (is (= (validate-iso-date-times {:a "2013-06-28T22:15:00.000+02:00" 
                                   :b "2013-07-01T22:15:00.000+02:00" 
                                   :c "field"} #{:a :b}) nil))
  (is (= (validate-iso-date-times {:a "2013-06-28T22:15:00.000+02:00" 
                                   :b "2013-07-01T22:1cc" 
                                   :c "field"} #{:a :b})
         (util/get-service-result :conflict "some date-time fields don't conform to ISO-8061 specification"))))
