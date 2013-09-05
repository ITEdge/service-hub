(ns itedge.service-hub.persistence-korma.util
  (:require [clojure.string :as string]
            [itedge.service-hub.core.util :as util]
            [korma.db :as db]
            [korma.core :as korma]
            [korma.sql.fns :as korma-fns]))

(defn- translate-wildcards
  "Translate string value with wildcards (*) into korma like expression ([like \"%something%\"])"
  [value]
  (if (.contains (str value) "*")
    (vector 'like (.replaceAll value "\\*" "%"))
    value))

(defn- translate
  "Translates CQL expression into its korma equivalent"
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
  "Translates CQL expressions into korma equivalents"
  [k v]
  (let [reduce-fn (fn [k acc expr] (conj acc {k (translate expr)}))]
    (reduce (partial reduce-fn k) '() v)))

(defn translate-relation-expressions
  "Translates CQL expressions into korma equivalents for relation queries"
  [criteria]
  (util/update-map-values criteria (fn [e] (if (map? e) (translate (first e)) e))))

(defn- process-expr
  "Processes expression (map entry) and returns list of korma expressions with one or more items"
  [expr]
  (let [k (key expr)
        v (val expr)]
    (if (map? v)
      (translate-expressions k v)
      (list {k (translate-wildcards v)}))))

(defn- construct-list
  "Constructs final query expression in form of list (expr1 expr2...)"
  [exprs]
  (let [reduce-fn (fn [acc expr]
                    (concat acc (process-expr expr)))]
    (reduce reduce-fn '() exprs)))

(defn construct-and-list
  "Constructs and list from list of expressions if there is more then one expression, otherwise returns first expression"
  [exprs]
  (let [exprs-list (construct-list exprs)
        exprs-count (count exprs-list)]
    (if (> exprs-count 1)
      (apply korma-fns/pred-and exprs-list)
      (first exprs-list))))

(defn- construct-relations
  "Constructs relations from attributes"
  [attributes]
  (into {} (map (fn [entry] 
                  (let [k (key entry)
                        v (val entry)]
                    (if (vector? v)
                      (hash-map k (into [] (map (fn [item] (if (map? item) (:id item) item)) v)))
                      (hash-map k (if (map? v) (:id v) v))))) attributes)))

(defn- extract-id
  "Extracts id from db insert results"
  [results]
  (if (map? results)
    (first (vals results))
    results))

(defn insert-entity-helper
  "Inserts new entity and all given relations, wraps operation in transaction"
  [entity attributes]
  (let [fields (select-keys attributes (:fields entity))
        relations (select-keys attributes (keys (:rel entity)))]
    (db/transaction
     (let [id (extract-id (korma/insert entity
                                        (korma/values fields)
                                        (korma/relations (construct-relations relations))))]
       (when-not (db/is-rollback?) (assoc attributes (:pk entity) id))))))

(defn update-entity-helper
  "Updates entity and all given relations, wraps operation in transaction"
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
  [entity id]
  (db/transaction
   (korma/delete entity
                 (korma/where {(:pk entity) id})
                 (korma/add-deletion-of-relations))))

(defn criteria-helper
  "Adds fields and relations criteria to query"
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
  [{:keys [ent] :as query} sorting-attrs] 
  (let [fields (conj (:fields ent) (:pk ent))]
    (reduce (fn [acc item]
              (if (util/in? fields (first item))
                (korma/order acc (first item) (second item))
                acc)) query sorting-attrs)))

(defn offset-helper
  "Adds offset attribute to query"
  [query from]
  (if from
    (korma/offset query from)
    query))

(defn limit-helper
  "Adds limit attribute to query"
  [query from to]
  (if (and from to)
    (korma/limit query (+ 1 (- to from)))
    query))

(defmacro select-single
  "Like korma.core/select, but selects only single (first result) from result set"
  [ent & body]
  `(let [query# (-> (korma/select* ~ent)
                    ~@body)]
     (first (korma/exec query#))))
