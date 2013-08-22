(ns itedge.service-hub.persistence-datomic.test.util
  (:require [datomic.api :as d :refer [q db entity create-database connect transact shutdown]]
            [itedge.service-hub.persistence-datomic.util :refer :all]
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
 
 {:db/id #db/id [:db.part/db]
  :db/ident :item/linked
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/one
  :db/doc "Linked articles"
  :db.install/_attribute :db.part/db}
])
@(d/transact conn schema-tx)
(def data-tx [
{:db/id #db/id[:db.part/user -1] :item/name "item 1" :item/count 7 :item/price 20.3}
{:db/id #db/id[:db.part/user -2] :item/name "item 2" :item/count 3 :item/price 11.9 :item/linked #db/id[:db.part/user -1]}
{:db/id #db/id[:db.part/user -3] :item/name "item 3" :item/count 9 :item/price 17.8 :item/linked #db/id[:db.part/user -2]}
])
@(d/transact conn data-tx)

(deftest test-add-entity
  (let [e (add-entity conn {:item/name "item 4" :item/count 5 :item/price 12.2})
        e-id (:db/id e)]
    (is (= e {:db/id e-id :item/name "item 4" :item/count 5 :item/price 12.2}))))

(deftest test-update-entity
  (let [e (first (list-entities (db conn) #{:item/name :item/count :item/price} {:item/name "item 4"} nil nil nil))
        e-id (:db/id e)
        updated-entity (update-entity conn {:db/id e-id :item/price 13.2})]
    (is (= updated-entity {:db/id e-id :item/name "item 4" :item/count 5 :item/price 13.2}))))

(deftest test-exist-entity?
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))]
    (is (= (exist-entity? (db conn) id) true))
    (is (= (exist-entity? (db conn) -1) false))))

(deftest test-get-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))]
    (is (= (:item/name (get-entity (db conn) id)) "item 1"))
    (is (= (:db/id (get-entity (db conn) id)) id))
    (is (= (get-entity (db conn) -1) nil))))

(deftest test-count-entities
  (is (= (count-entities (db conn) #{:item/name :item/count :item/price} nil) 4))
  (is (= (count-entities (db conn) #{:item/name :item/count :item/price} {:item/name "item 4"}) 1))
  (is (= (count-entities (db conn) #{:item/name :item/count :item/price} {:item/name "item*"}) 4)))

(deftest test-list-entities
  (let [q-result-1 (list-entities (db conn) #{:item/name :item/count :item/price} nil nil nil nil)
        q-result-2 (list-entities (db conn) #{:item/name :item/count :item/price} {:item/name "item*"} [[:item/name :ASC]] 0 2)
        q-result-2-ent-1 (dissoc (first q-result-2) :db/id)]
    (is (= (count q-result-1) 4))
    (is (= (count q-result-2) 2))
    (is (= q-result-2-ent-1 {:item/name "item 1" :item/count 7 :item/price 20.3}))))

(shutdown false)

