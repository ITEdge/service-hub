(ns itedge.service-hub.persistence-datomic.test.handlers-datomic
  (:require [datomic.api :as d :refer [q db entity create-database connect transact shutdown]]
            [itedge.service-hub.persistence-datomic.handlers-datomic :as handlers-datomic]
            [itedge.service-hub.core.handlers :refer :all]
            [clojure.test :refer :all]))

(def uri "datomic:mem://test-handlers")
(create-database uri)
(def conn (connect uri))
(def schema-tx (read-string (slurp "dev-resources/schema.edn")))
@(d/transact conn schema-tx)
(def data-tx (read-string (slurp "dev-resources/data.edn")))
@(d/transact conn data-tx)

(def datomic-handler (handlers-datomic/create-handler #{:item/name :item/count :item/price}))

(deftest test-handle-find-entity
  (let [datasource (db conn)
        id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] datasource)))
        id-2 (first (first (q '[:find ?e :where [?e :item/name "item 2"]] datasource)))
        id-3 (first (first (q '[:find ?e :where [?e :item/name "item 3"]] datasource)))
        id-5 (first (first (q '[:find ?e :where [?e :item/name "item 5"]] datasource)))
        e (handle-find-entity datomic-handler id datasource)
        e-2 (handle-find-entity datomic-handler id-2 datasource)
        e-3 (handle-find-entity datomic-handler id-3 datasource)
        e-5 (handle-find-entity datomic-handler id-5 datasource)]
    (is (= e {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= e (handle-find-entity datomic-handler (:item/linked e-2) datasource)))
    (is (= e-3 (handle-find-entity datomic-handler (:item/linked e-5) datasource)))
    (is (= e-5 (handle-find-entity datomic-handler (:item/linked e-3) datasource)))))

(deftest test-handle-add-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] (db conn))))
        e (handle-find-entity datomic-handler id (db conn))
        new-e (handle-add-entity datomic-handler {:item/name "item 6" :item/count 8 
                                                  :item/price 16.7 :item/linked (:db/id e)} conn)
        new-e2 (handle-add-entity datomic-handler {:item/name "item 7" :item/count 6
                                                   :item/price 19.8 :item/linked (:item/linked e)} conn)]
    (is (= (handle-find-entity datomic-handler (:item/linked new-e) (db conn)) e))
    (is (= (-> new-e2 :item/linked :item/name) "item 1"))))

(deftest test-count-entities
  (let [datasource (db conn)
        id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] datasource)))
        e (handle-find-entity datomic-handler id datasource)]
    (is (= (handle-count-entities datomic-handler {:db/id id} datasource) 1))
    (is (= (handle-count-entities datomic-handler {:db/id (:item/linked e)} datasource) 1))
    (is (= (handle-count-entities datomic-handler {:item/linked (:item/linked e)} datasource) 1))))

(deftest test-list-entities
  (let [datasource (db conn)
        id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] datasource)))
        e (handle-find-entity datomic-handler id datasource)
        q-result (handle-list-entities datomic-handler {:item/linked (:item/linked e) :item/name "item 2"} nil nil nil datasource)
        q-result-1 (first q-result)]
    (is (= q-result-1 e))))

(deftest test-list-entity-history
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 3"]] (db conn))))
        updated-entity (handle-update-entity datomic-handler {:db/id id :item/count 10} conn)
        [former-e later-e] (handle-list-entity-history datomic-handler id nil [[:t :ASC]] nil nil (db conn))]
    (is (= (dissoc later-e :t) updated-entity))
    (is (= (:item/count former-e) 9))
    (is (= (handle-count-entity-history datomic-handler id nil (db conn)) 2))))

(deftest test-find-entity-history
  (let [datasource (db conn)
        id (first (first (q '[:find ?e :where [?e :item/name "item 3"]] datasource)))
        [former-e later-e] (handle-list-entity-history datomic-handler id nil [[:t :ASC]] nil nil datasource)]
    (is (= (dissoc former-e :t) (handle-find-entity-history datomic-handler id (:t former-e) datasource)))
    (is (= (dissoc later-e :t) (handle-find-entity-history datomic-handler id (:t later-e) datasource)))))

(shutdown false)


