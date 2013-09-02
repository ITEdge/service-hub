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
        e (handle-find-entity datomic-handler id)
        e-2 (handle-find-entity datomic-handler id-2)]
    (is (= e {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))
    (is (= e (handle-find-entity datomic-handler (:item/linked e-2))))))

(deftest test-handle-add-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 2"]] (db conn))))
        e (handle-find-entity datomic-handler id)
        new-e (handle-add-entity datomic-handler {:item/name "item 6" :item/count 8 
                                                  :item/price 16.7 :item/linked (:db/id e)})
        new-e2 (handle-add-entity datomic-handler {:item/name "item 7" :item/count 6
                                                   :item/price 19.8 :item/linked (:item/linked e)})]
    (is (= (handle-find-entity datomic-handler (:item/linked new-e)) e))
    (is (= (-> new-e2 :item/linked :item/name) "item 1"))))

(shutdown false)
