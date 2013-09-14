(ns itedge.service-hub.persistence-datomic.util
  (:require [datomic.api :as d :refer [q db entity transact tempid resolve-tempid 
                                       as-of history tx->t t->tx]]
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
  "Determines if entity with given minimal fieldset and id exists in database"
  [db fieldset id]
  (let [query (-> '[:find ?eid :in $ ?eid :where [?eid]]
                  (add-fieldset '?eid fieldset))] 
    (if-let [e (extract-single (q query db id))]
      true
      false)))

(defn get-entity
  "Gets entity with specified minimal fieldset and id, if no such entity exists, returns nil"
  [db fieldset id]
  (when (exist-entity? db fieldset id)
    (convert-entity-to-map (entity db id))))

(defn get-entity-history
  "Gets entity with specified minimal fieldset, id and history id, if no such entity exists, returns nil"
  [db fieldset id history-id]
  (get-entity (as-of db (t->tx history-id)) fieldset id))

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
  "Strips entity with given minimal fieldset and id of all its attributes"
  [conn fieldset id]
  (when (exist-entity? (db conn) fieldset id)
    (:db/id (entity (:db-after @(transact conn [[:db.fn/retractEntity id]])) id))))

(defn- list-entities-q-a
  [db fieldset criteria id-key]
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

(defn count-entities
  "Counts entities with given minimal fieldset in specified db"
  [db fieldset criteria id-key]
  (let [[query args] (list-entities-q-a db fieldset criteria id-key)]
    (count (apply q query args))))

(defn- map-entities
  [entity-ids db]
  (map (fn [[e-id]] 
         (convert-entity-to-map (entity db e-id)))
       entity-ids))

(defn- list-history-entities-q-a
  [db fieldset criteria id-key]
  (let [entity-symbol '?e
        transaction-symbol '?t
        in-clause (if (contains? criteria id-key) [:in '$ entity-symbol] [:in '$])
        query-args (if (contains? criteria id-key) (list db (id-key criteria)) (list db))
        criteria (dissoc criteria id-key)
        where-clauses (-> [:where]
                          (add-fieldset entity-symbol (unrestricted-fields fieldset criteria))
                          (add-criteria entity-symbol criteria)
                          (conj [entity-symbol '_ '_ transaction-symbol]))
        query (into [] (-> [:find entity-symbol transaction-symbol]
                           (concat in-clause)
                           (concat where-clauses)))]
    [query query-args]))

(defn count-history-entities
  "Counts entities histories with given minimal fieldset in specified db"
  [db fieldset criteria id-key]
  (let [[query args] (list-history-entities-q-a (history db) fieldset criteria id-key)]
    (count (apply q query args))))

(defn- map-history-entities
  [entity-tx-ids db]
  (map (fn [[e-id tx-id]]
         (-> (convert-entity-to-map (entity (as-of db tx-id) e-id))
             (assoc :t (tx->t tx-id))))
       entity-tx-ids))

(defn list-entities-p
  "Processes list entities query in specified db, with map-fn, sorts and paginates the result"
  [db map-fn [query query-args] sort-attrs from to]
  (let [query-result (apply q query query-args)
        unsorted-entities (map-fn (sort query-result) db)]
    (if (> (count unsorted-entities) 0)
      (if (seq sort-attrs)
        (util/get-ranged-vector (util/sort-maps unsorted-entities sort-attrs) from to)
        (util/get-ranged-vector unsorted-entities from to))
      [])))

(defn list-entities
  "Lists entities with given minimal fieldset, criteria, sorting and paging in specified db"
  [db fieldset criteria id-key sort-attrs from to]
  (list-entities-p db map-entities (list-entities-q-a db fieldset criteria id-key) sort-attrs from to))

(defn list-entities-with-history
  "Lists entities (including history under :t key) with given minimal fieldset, criteria, sorting and paging in specified db"
  [db fieldset criteria id-key sort-attrs from to]
  (list-entities-p db map-history-entities (list-history-entities-q-a (history db) fieldset criteria id-key) sort-attrs from to))

