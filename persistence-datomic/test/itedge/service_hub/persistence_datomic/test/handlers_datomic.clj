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

(deftest test-handle-get-entity
  (let [id (first (first (q '[:find ?e :where [?e :item/name "item 1"]] (db conn))))
        e (handle-find-entity datomic-handler id)]
    (is (= e {:db/id id :item/name "item 1" :item/count 7 :item/price 20.3 :item/codes #{"C1" "C2"}}))))

(shutdown false)
