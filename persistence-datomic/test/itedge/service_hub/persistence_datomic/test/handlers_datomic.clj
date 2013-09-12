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

(def datomic-handler (handlers-datomic/create-handler conn #{:item/name :item/count :item/price}))

(deftest test-handle-find-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))
        id-2 (first (first (q '[:find ?e :where [?e :item/name "item 2"]] (db conn))))
        id-3 (first (first (q '[:find ?e :where [?e :item/name "item 3"]] (db conn))))
        id-5 (first (first (q '[:find ?e :where [?e :item/name "item 5"]] (db conn))))
        e (handle-find-entity datomic-handler id)
        e-2 (handle-find-entity datomic-handler id-2)
        e-3 (handle-find-entity datomic-handler id-3)
        e-5 (handle-find-entity datomic-handler id-5)]
    (is (= e {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= e (handle-find-entity datomic-handler (:item/linked e-2))))
    (is (= e-3 (handle-find-entity datomic-handler (:item/linked e-5))))
    (is (= e-5 (handle-find-entity datomic-handler (:item/linked e-3))))))

(deftest test-handle-add-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] (db conn))))
        e (handle-find-entity datomic-handler id)
        new-e (handle-add-entity datomic-handler {:item/name "item 6" :item/count 8 
                                                  :item/price 16.7 :item/linked (:db/id e)})
        new-e2 (handle-add-entity datomic-handler {:item/name "item 7" :item/count 6
                                                   :item/price 19.8 :item/linked (:item/linked e)})]
    (is (= (handle-find-entity datomic-handler (:item/linked new-e)) e))
    (is (= (-> new-e2 :item/linked :item/name) "item 1"))))

(deftest test-count-entities
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] (db conn))))
        e (handle-find-entity datomic-handler id)]
    (is (= (handle-count-entities datomic-handler {:db/id id}) 1))
    (is (= (handle-count-entities datomic-handler {:db/id (:item/linked e)}) 1))
    (is (= (handle-count-entities datomic-handler {:item/linked (:item/linked e)}) 1))))

(deftest test-list-entities
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] (db conn))))
        e (handle-find-entity datomic-handler id)
        q-result (handle-list-entities datomic-handler {:item/linked (:item/linked e) :item/name "item 2"} nil nil nil)
        q-result-1 (first q-result)]
    (is (= q-result-1 e))))

(deftest test-list-entity-history
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 3"]] (db conn))))
        updated-entity (handle-update-entity datomic-handler {:db/id id :item/count 10})
        [former-e later-e] (handle-list-entity-history datomic-handler id nil [[:t :ASC]] nil nil)]
    (is (= (dissoc later-e :t) updated-entity))
    (is (= (:item/count former-e) 9))))

(deftest test-find-entity-history
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 3"]] (db conn))))
        [former-e later-e] (handle-list-entity-history datomic-handler id nil [[:t :ASC]] nil nil)]
    (is (= (dissoc former-e :t) (handle-find-entity-history datomic-handler id (:t former-e))))
    (is (= (dissoc later-e :t) (handle-find-entity-history datomic-handler id (:t later-e))))))

(shutdown false)


