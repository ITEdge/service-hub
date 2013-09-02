(ns itedge.service-hub.persistence-datomic.test.util
  (:require [datomic.api :as d :refer [q db entity create-database connect transact shutdown]]
            [itedge.service-hub.persistence-datomic.util :refer :all]
            [clojure.test :refer :all]))

(def uri "datomic:mem://test-util")
(create-database uri)
(def conn (connect uri))
(def schema-tx (read-string (slurp "dev-resources/schema.edn")))
@(d/transact conn schema-tx)
(def data-tx (read-string (slurp "dev-resources/data.edn")))
@(d/transact conn data-tx)

(def item-fieldset #{:item/name :item/count :item/price})

(deftest test-add-entity
  (let [e (add-entity conn {:item/name "item 4" :item/count 5 :item/price 12.2 :item/codes #{"C1" "C2"}})
        e-id (:db/id e)]
    (is (= e {:db/id e-id :item/name "item 4" :item/count 5 :item/price 12.2 :item/codes #{"C1" "C2"}}))))

(deftest test-update-entity
  (let [e (first (list-entities (db conn) item-fieldset {:item/name "item 4"} :db/id nil nil nil))
        e-id (:db/id e)
        updated-entity (update-entity conn {:db/id e-id :item/price 13.2})]
    (is (= updated-entity {:db/id e-id :item/name "item 4" :item/count 5 :item/price 13.2 :item/codes #{"C1" "C2"}}))))

(deftest test-delete-entity
  (let [e (first (list-entities (db conn) item-fieldset {:item/name "item 2"} :db/id nil nil nil))
        e-id (:db/id e)
        ret-id (delete-entity conn item-fieldset e-id)]
    (is (= ret-id e-id))))

(deftest test-exist-entity?
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))]
    (is (= (exist-entity? (db conn) item-fieldset id) true))
    (is (= (exist-entity? (db conn) item-fieldset -1) false))))

(deftest test-get-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))] 
    (is (= (get-entity (db conn) item-fieldset id) 
           {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= (get-entity (db conn) item-fieldset -1) nil))))

(deftest test-count-entities
  (is (= (count-entities (db conn) item-fieldset nil :db/id) 4))
  (is (= (count-entities (db conn) item-fieldset {:item/name "item 4"} :db/id) 1))
  (is (= (count-entities (db conn) item-fieldset {:item/name "item*" :item/codes "C1"} :db/id) 2))
  (is (= (count-entities (db conn) item-fieldset {:db/id 999} :db/id) 0)))

(deftest test-list-entities
  (let [q-result-1 (list-entities (db conn) item-fieldset nil :db/id nil nil nil)
        q-result-2 (list-entities (db conn) item-fieldset {:item/name "item*"} :db/id [[:item/name :ASC]] 0 2)
        q-result-3 (list-entities (db conn) item-fieldset {:db/id (:db/id (first q-result-1))} :db/id nil nil nil)
        q-result-2-ent (dissoc (first q-result-2) :db/id)]
    (is (= (count q-result-1) 4))
    (is (= (count q-result-2) 2))
    (is (= (count q-result-3) 1))
    (is (= q-result-2-ent {:item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))))

(shutdown false)


