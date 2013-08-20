(ns itedge.service-hub.persistence-datomic.util
  (:require [datomic.api :as d :refer [q entity touch transact]]
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
    (touch (entity db id))))

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
                (add-criteria '? criteria))]
    (extract-single (q query db))))

(defn list-entities
  "List entities with given fieldset, criteria, sorting and paging in specified db"
  [db fieldset criteria sort-attrs from to]
  (let [entity-symbol '?e
        query (-> [:find entity-symbol :where]
                (add-fieldset entity-symbol (unrestricted-fields fieldset criteria))
                (add-criteria entity-symbol criteria))
        entity-ids (q query db)]
    (map (fn [r] (touch (entity db (first r)))) entity-ids)))


