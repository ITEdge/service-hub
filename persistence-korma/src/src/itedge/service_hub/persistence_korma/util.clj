(ns itedge.service-hub.persistence-korma.util
  (:require [clojure.string :as string]
            [itedge.service-hub.core.util :as util]
            [korma.db :as db]
            [korma.core :as korma]
            [korma.sql.fns :as korma-fns]))

(defn- translate-wildcards
  "Translate string value with wildcards (*) into korma like expression ([like \"%something%\"])"
  {:added "EBS 1.0"}
  [value]
  (if (.contains (str value) "*")
    (vector 'like (.replaceAll value "\\*" "%"))
    value))

(defn- translate
  "Translate CQL expression into korma equivalent"
  {:added "EBS 1.0"}
  [expr]
  (let [k-e (key expr)
        v-e (val expr)
        translated (get {:not korma-fns/pred-not= 
                         :not-in korma-fns/pred-not-in
                         :in korma-fns/pred-in 
                         :gt korma-fns/pred-> 
                         :lt korma-fns/pred-<
                         :gteq korma-fns/pred->= 
                         :lteq korma-fns/pred-<=} 
                         k-e k-e)]
    [translated (translate-wildcards v-e)]))

(defn- translate-expressions
  "Translate CQL expressions into korma equivalents"
  {:added "EBS 1.0"}
  [k v]
  (let [reduce-fn (fn [k acc expr] (conj acc {k (translate expr)}))]
    (reduce (partial reduce-fn k) '() v)))

(defn translate-relation-expressions
  "Translate CQL expressions into korma equivalents for relation queries"
  {:added "EBS 1.0"}
  [criteria]
  (util/update-map-values criteria (fn [e] (if (map? e) (translate (first e)) e))))

(defn- process-expr
  "Process expression (map entry) and return list of korma expressions with one or more items"
  {:added "EBS 1.0"}
  [expr]
  (let [k (key expr)
        v (val expr)]
    (if (map? v)
      (translate-expressions k v)
      (list {k (translate-wildcards v)}))))

(defn- construct-list
  "Construct final query expression in form of list (expr1 expr2...)"
  {:added "EBS 1.0"}
  [exprs]
  (let [reduce-fn (fn [acc expr]
                    (concat acc (process-expr expr)))]
    (reduce reduce-fn '() exprs)))

(defn construct-and-list
  "construct and list from list of expressions if there is more then one expression, otherwise return first expression"
  {:added "EBS 1.0"}
  [exprs]
  (let [exprs-list (construct-list exprs)
        exprs-count (count exprs-list)]
    (if (> exprs-count 1)
      (apply korma-fns/pred-and exprs-list)
      (first exprs-list))))

(defn- construct-relations
  "Construct relations from attributes"
  {:added "EBS 1.0"}
  [attributes]
  (into {} (map (fn [entry] 
                  (let [k (key entry)
                        v (val entry)]
                    (if (vector? v)
                      (hash-map k (into [] (map (fn [item] (if (map? item) (:id item) item)) v)))
                      (hash-map k (if (map? v) (:id v) v))))) attributes)))

(defn- extract-id
  "Extract id from db insert results"
  {:added "EBS 1.0"}
  [results]
  (if (map? results)
    (first (vals results))
    results))

(defn insert-entity-helper
  "Inserts new entity and all given relations, wraps operation in transaction"
  {:added "EBS 1.0"}
  [entity attributes]
  (let [fields (select-keys attributes (:fields entity))
        relations (select-keys attributes (keys (:rel entity)))]
	  (db/transaction
     (let [id (extract-id
					      (korma/insert entity
					        (korma/values fields)
					        (korma/relations (construct-relations relations))))]
       (when-not (db/is-rollback?) (assoc attributes (:pk entity) id))))))

(defn update-entity-helper
  "Updates entity and all given relations, wraps operation in transaction"
  {:added "EBS 1.0"}
  [entity attributes]
  (let [fields (select-keys attributes (:fields entity))
        relations (select-keys attributes (keys (:rel entity)))]
    (db/transaction
      (korma/update entity
        (korma/set-fields fields)
        (korma/where {(:pk entity) ((:pk entity) attributes)})
        (korma/relations (construct-relations relations)))
      (when-not (db/is-rollback?) attributes))))

(defn delete-entity-helper
  "Deletes entity along with all relations, wraps operation in transaction"
  {:added "EBS 1.0"}
  [entity id]
  (db/transaction
	  (korma/delete entity
	    (korma/where {(:pk entity) id})
	    (korma/add-deletion-of-relations))))

(defn criteria-helper
  "Adds fields and relations criteria to query"
  {:added "EBS 1.0"}
  [{:keys [ent] :as query} criteria]
  (let [fields (conj (:fields ent) (:pk ent))
        fields-criteria (select-keys criteria fields)
        relations-criteria (select-keys criteria (keys (:rel ent)))]
    (-> query
      ((fn [query]
        (if (empty? fields-criteria)
          query
          (korma/where query (construct-and-list fields-criteria)))))
      ((fn [query]
        (if (empty? relations-criteria)
          query
          (korma/where-relations query (translate-relation-expressions relations-criteria))))))))

(defn sorting-helper
  "Adds sorting attributes to query"
  {:added "EBS 1.0"}
  [{:keys [ent] :as query} sorting-attrs] 
  (let [fields (conj (:fields ent) (:pk ent))]
    (reduce (fn [acc item]
      (if (util/in? fields (first item))
        (korma/order acc (first item) (second item))
        acc)) query sorting-attrs)))

(defn offset-helper
  "Adds offset attribute to query"
  {:added "EBS 1.0"}
  [query from]
  (if from
    (korma/offset query from)
    query))

(defn limit-helper
  "Adds limit attribute to query"
  {:added "EBS 1.0"}
  [query from to]
  (if (and from to)
    (korma/limit query (+ 1 (- to from)))
    query))

(defmacro select-single
  "Like korma.core/select, but selects only single (first result) from result set"
  {:added "EBS 1.0"}
  [ent & body]
  `(let [query# (-> (korma/select* ~ent)
                 ~@body)]
     (first (korma/exec query#))))