(ns itedge.service-hub.persistence-datomic.util
  (:require [datomic.api :as d :refer [q db entity transact tempid resolve-tempid]]
            [clojure.set :as set]
            [itedge.service-hub.core.util :as util]))

(defn convert-entity-to-map 
  "Converts datomic.Entity to clojure persistent map, 
   doing only flat conversion, so nested entities are not converted"
  [dynamic-map]
  (let [m (reduce (fn [acc [k v]] (assoc acc k v)) {} dynamic-map)]
    (assoc m :db/id (:db/id dynamic-map))))

(defn- extract-single [result]
  (first (first result)))

(defn- add-fieldset [query entity-symbol fieldset]
  (reduce (fn [acc item] (conj acc [entity-symbol item])) query fieldset))

(defn exist-entity? 
  "Determines if entity with given fieldset and id exists in database"
  [db fieldset id]
  (let [query (-> '[:find ?eid :in $ ?eid :where [?eid]]
                  (add-fieldset '?eid fieldset))] 
    (if-let [e (extract-single (q query db id))]
      true
      false)))

(defn get-entity
  "Gets entity with specified fieldset and id, if no such entity exists, returns nil"
  [db fieldset id]
  (when (exist-entity? db fieldset id)
    (convert-entity-to-map (entity db id))))

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
  (reduce (fn [acc [k v]]
            (let [item-symbol (gensym "?item")]
              (-> acc 
                  (conj [entity-symbol k item-symbol]) 
                  (conj (get-function-expression v item-symbol))))) query criteria))

(defn- unrestricted-fields [fieldset criteria]
  (set/difference fieldset (set (keys criteria))))

(defn add-entity
  "Adds entity with given attributes in the specified connection"
  [conn attributes]
  (let [temp-id (tempid :db.part/user -1)
        tx-attributes (assoc attributes :db/id temp-id)
        tx-result @(transact conn [tx-attributes])
        final-id (resolve-tempid (db conn) (:tempids tx-result) temp-id)]
    (convert-entity-to-map (entity (:db-after tx-result) final-id))))

(defn update-entity
  "Updates entity with given attributes in the specified connection"
  [conn attributes]
  (when-let [id (:db/id attributes)]
    (convert-entity-to-map (entity (:db-after @(transact conn [attributes])) id))))

(defn delete-entity
  "Strips entity with given fieldset and id of all its attributes"
  [conn fieldset id]
  (when (exist-entity? (db conn) fieldset id)
    (:db/id (entity (:db-after @(transact conn [[:db.fn/retractEntity id]])) id))))

(defn count-entities
  "Counts entities with given fieldset in specified db"
  [db fieldset criteria id-key]
  (let [entity-symbol '?e
        count-clause (-> '() (conj entity-symbol) (conj 'count))
        in-clause (if (contains? criteria id-key) [:in '$ entity-symbol] [:in '$])
        query-args (if (contains? criteria id-key) (list db (id-key criteria)) (list db))
        criteria (dissoc criteria id-key)
        where-clauses (-> [:where]
                          (add-fieldset entity-symbol (unrestricted-fields fieldset criteria))
                          (add-criteria entity-symbol criteria))
        query (into [] (-> [:find count-clause]
                           (concat in-clause)
                           (concat where-clauses)))
        result (extract-single (apply q query query-args))]
    (if result result 0)))

(defn- list-entities-q-a
  [fieldset criteria id-key db]
  (let [entity-symbol '?e
        in-clause (if (contains? criteria id-key) [:in '$ entity-symbol] [:in '$])
        query-args (if (contains? criteria id-key) (list db (id-key criteria)) (list db))
        criteria (dissoc criteria id-key)
        where-clauses (-> [:where]
                          (add-fieldset entity-symbol (unrestricted-fields fieldset criteria))
                          (add-criteria entity-symbol criteria))
        query (into [] (-> [:find entity-symbol]
                           (concat in-clause)
                           (concat where-clauses)))]
    [query query-args]))

(defn list-entities-p
  "Processes list entities query in specified db, sorts and paginates the result"
  [db [query query-args] sort-attrs from to]
  (let [entity-ids (apply q query query-args)
        unsorted-entities (map (fn [r] (convert-entity-to-map (entity db (first r)))) 
                               (sort entity-ids))]
    (if (> (count unsorted-entities) 0)
      (if (seq sort-attrs)
        (util/get-ranged-vector (util/sort-maps unsorted-entities sort-attrs) from to)
        (util/get-ranged-vector unsorted-entities from to))
      [])))

(defn list-entities
  "Lists entities with given fieldset, criteria, sorting and paging in specified db"
  [db fieldset criteria id-key sort-attrs from to]
  (list-entities-p db (list-entities-q-a fieldset criteria id-key db) sort-attrs from to))


