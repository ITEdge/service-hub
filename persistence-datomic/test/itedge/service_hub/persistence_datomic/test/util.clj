(ns itedge.service-hub.persistence-datomic.test.util
  (:require [datomic.api :as d :refer [q db entity create-database connect transact shutdown]]
            [itedge.service-hub.persistence-datomic.util :refer :all]
            [itedge.service-hub.core.handlers :refer :all]
            [clojure.test :refer :all]))

(def uri "datomic:mem://test")
(create-database uri)
(def conn (connect uri))
(def schema-tx [
{:db/id #db/id[:db.part/db]
  :db/ident :item/name
  :db/unique :db.unique/value
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
  :db/fulltext true
  :db/doc "A items's name"
  :db.install/_attribute :db.part/db}

 {:db/id #db/id[:db.part/db]
  :db/ident :item/count
  :db/valueType :db.type/long
  :db/cardinality :db.cardinality/one
  :db/doc "A item's count"
  :db.install/_attribute :db.part/db}
 
 {:db/id #db/id[:db.part/db]
  :db/ident :item/price
  :db/valueType :db.type/double
  :db/cardinality :db.cardinality/one
  :db/doc "A item's price"
  :db.install/_attribute :db.part/db}

 {:db/id #db/id[:db.part/db]
  :db/ident :item/codes
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/many
  :db/fulltext true
  :db/doc "A items's code"
  :db.install/_attribute :db.part/db}
 
 {:db/id #db/id [:db.part/db]
  :db/ident :item/linked
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/one
  :db/doc "Linked articles"
  :db.install/_attribute :db.part/db}

 {:db/id #db/id[:db.part/db]
  :db/ident :item/lineItems
  :db/isComponent true
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/many
  :db/doc "line item"
  :db.install/_attribute :db.part/db}

 {:db/id #db/id [:db.part/db]
  :db/ident :lineItem/code
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
  :db/doc "Line item code"
  :db.install/_attribute :db.part/db}
])
@(d/transact conn schema-tx)
(def data-tx [
{:db/id #db/id[:db.part/user -1] :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}
{:db/id #db/id[:db.part/user -2] :item/name "item 2" :item/count 3 :item/price 11.9 :item/linked #db/id[:db.part/user -1]}
{:db/id #db/id[:db.part/user -3] :item/name "item 3" :item/count 9 :item/price 17.8 :item/linked #db/id[:db.part/user -2]}
])
@(d/transact conn data-tx)

(def item-fieldset #{:item/name :item/count :item/price})

(def datomic-handler (create-handler conn item-fieldset))

(deftest test-add-entity
  (let [e (add-entity conn {:item/name "item 4" :item/count 5 :item/price 12.2 :item/codes #{"C1" "C2"}})
        e-id (:db/id e)
        e-h (handle-add-entity datomic-handler {:item/name "item 5" :item/count 7 :item/price 17.1 :item/linked e-id :item/codes #{"C2" "C3"}})
        e-h-id ((handle-get-unique-identifier datomic-handler) e-h)]
    (is (= e {:db/id e-id :item/name "item 4" :item/count 5 :item/price 12.2 :item/codes #{"C1" "C2"}}))
    (is (= e-h {:db/id e-h-id :item/name "item 5" :item/count 7 :item/price 17.1 :item/linked e-id :item/codes #{"C2" "C3"}}))))

(deftest test-update-entity
  (let [e (first (list-entities (db conn) item-fieldset {:item/name "item 4"} nil nil nil))
        e-h (first (list-entities (db conn) item-fieldset {:item/name "item 5"} nil nil nil))
        e-id (:db/id e)
        e-h-id ((handle-get-unique-identifier datomic-handler) e-h)
        updated-entity (update-entity conn {:db/id e-id :item/price 13.2})
        updated-h-entity (handle-update-entity datomic-handler {:db/id e-h-id :item/price 18.1})]
    (is (= updated-entity {:db/id e-id :item/name "item 4" :item/count 5 :item/price 13.2 :item/codes #{"C1" "C2"}}))
    (is (= updated-h-entity {:db/id e-h-id :item/name "item 5" :item/count 7 :item/price 18.1 :item/linked e-id :item/codes #{"C2" "C3"}}))))

(deftest test-delete-entity
  (let [e (first (list-entities (db conn) item-fieldset {:item/name "item 2"} nil nil nil))
        e-h (first (list-entities (db conn) item-fieldset {:item/name "item 3"} nil nil nil))
        e-id (:db/id e)
        e-h-id ((handle-get-unique-identifier datomic-handler) e-h)
        ret-id (delete-entity conn item-fieldset e-id)
        ret-h-id (handle-delete-entity datomic-handler e-h-id)]
    (is (= ret-id e-id))
    (is (= ret-h-id e-h-id))))

(deftest test-exist-entity?
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))]
    (is (= (exist-entity? (db conn) item-fieldset id) true))
    (is (= (exist-entity? (db conn) item-fieldset -1) false))
    (is (= (handle-exist-entity datomic-handler id) true))
    (is (= (handle-exist-entity datomic-handler -1) false))))

(deftest test-get-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))] 
    (is (= (get-entity (db conn) item-fieldset id) 
           {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= (get-entity (db conn) item-fieldset -1) nil))
    (is (= (handle-find-entity datomic-handler id)
           {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= (handle-find-entity datomic-handler -1) nil))))

(deftest test-count-entities
  (is (= (count-entities (db conn) item-fieldset nil) 3))
  (is (= (count-entities (db conn) item-fieldset {:item/name "item 4"}) 1))
  (is (= (count-entities (db conn) item-fieldset {:item/name "item*" :item/codes "C1"}) 2))
  (is (= (handle-count-entities datomic-handler nil) 3))
  (is (= (handle-count-entities datomic-handler {:item/name "item 4"}) 1))
  (is (= (handle-count-entities datomic-handler {:item/name "item*" :item/lineItems "NN"}) 0)))

(deftest test-list-entities
  (let [q-result-1 (list-entities (db conn) item-fieldset nil nil nil nil)
        q-result-1-h (handle-list-entities datomic-handler nil nil nil nil)
        q-result-2 (list-entities (db conn) item-fieldset {:item/name "item*"} [[:item/name :ASC]] 0 2)
        q-result-2-h (handle-list-entities datomic-handler {:item/name "item*"} [[:item/name :ASC]] 0 2)
        q-result-2-ent (dissoc (first q-result-2) :db/id)
        q-result-2-h-ent (dissoc (first q-result-2-h) :db/id)]
    (is (= (count q-result-1) 3))
    (is (= (count q-result-2) 2))
    (is (= q-result-2-ent {:item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= (count q-result-1-h) 3))
    (is (= (count q-result-2-h) 2))
    (is (= q-result-2-h-ent {:item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))))

;(shutdown false)


