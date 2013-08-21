(ns itedge.service-hub.persistence-datomic.util
  (:require [datomic.api :as d :refer [q entity transact]]
            [clojure.set :as set]
            [itedge.service-hub.core.util :as util]))

(defn- extract-single [result]
  (first (first result)))

(defn exist-entity? 
  "Determines if entity with given id exist in database"
  [db id]
  (if-let [e (extract-single (q '[:find ?eid :in $ ?eid :where [?eid]] db id))]
    true
    false))

(defn get-entity
  "Get entity with specified id, if no such entity exist, returns nil"
  [db id]
  (when (exist-entity? db id)
    (entity db id)))

(defn- not-compare-w [v c-v]
  (if (coll? v) (not (util/in? v c-v)) (not (util/wildcard-compare v c-v))))

(defn- not-in-compare [v c-v]
  (not (util/in? c-v v)))

(defn- in-compare [v c-v]
  (util/in? c-v v))

(defn- compare-w [v c-v]
  (if (coll? v) (util/in? v c-v) (util/wildcard-compare v c-v)))

(defn- get-function-expression [expression-map item-symbol]
  (let [expression (if (map? expression-map) (first expression-map) (first {:value expression-map}))
        expression-key (key expression)
        compare-value (val expression)
        func-body (-> '()
                    (conj compare-value)
                    (conj item-symbol))]
	  (expression-key {:not [(conj func-body not-compare-w)]
                           :not-in [(conj func-body not-in-compare)]
	                   :in [(conj func-body in-compare)]
	                   :gt [(conj func-body >)] 
	                   :lt [(conj func-body <)]
	                   :gteq [(conj func-body >=)]
	                   :lteq [(conj func-body <=)]
	                   :value [(conj func-body compare-w)]}))) 

(defn- add-criteria [query entity-symbol criteria]
  (reduce (fn [acc item]
            (let [item-symbol (gensym "?item")]
              (-> query 
                (conj [entity-symbol (key item) item-symbol]) 
                (conj (get-function-expression (val item) item-symbol))))) query criteria))

(defn- add-fieldset [query entity-symbol fieldset]
  (reduce (fn [acc item] (conj query [entity-symbol item])) query fieldset))

(defn- unrestricted-fields [fieldset criteria]
  (set/difference fieldset (set (keys criteria))))

(defn count-entities
  "Count entities with given fieldset in specified db"
  [db fieldset criteria]
  (let [query (-> '[:find (count ?e) :where]
                (add-fieldset '?e (unrestricted-fields fieldset criteria))
                (add-criteria '?e criteria))]
    (extract-single (q query db))))

(defn- list-entities-q
  [fieldset criteria]
  (-> '[:find ?e :where]
      (add-fieldset '?e (unrestricted-fields fieldset criteria))
      (add-criteria '?e criteria)))

(defn list-entities-p
  "Process list entities query in specified db, sorts and paginates the result"
  [db query sort-attrs from to]
  (let [entity-ids (q query db)
        unsorted-entities (map (fn [r] (entity db (first r))) (sort entity-ids))]
    (if (seq sort-attrs)
      (util/get-ranged-vector (util/sort-maps unsorted-entities sort-attrs) from to)
      (util/get-ranged-vector unsorted-entities from to))))

(defn list-entities
  "List entities with given fieldset, criteria, sorting and paging in specified db"
  [db fieldset criteria sort-attrs from to]
  (list-entities-p db (list-entities-q fieldset criteria) sort-attrs from to))
