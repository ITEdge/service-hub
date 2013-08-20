(ns itedge.service-hub.core.handlers-memory
  (:require [itedge.service-hub.core.handlers :refer :all]
            [itedge.service-hub.core.util :as util]))

(defn- index-data [data id-key index]
  (reduce (fn [acc item]
            (let [next-id (swap! index inc)]
              (assoc acc next-id (assoc item id-key next-id)))) {} data))

(defn- wildcard-compare [compare-value wildcard-value]
  (if (and (= (type wildcard-value) String) (= (type compare-value) String) (>= (.length wildcard-value) 2))
    (cond
      (and 
        (.startsWith wildcard-value "*") 
	(.endsWith wildcard-value "*")) (let [search-term (.substring wildcard-value 1 (- (.length wildcard-value) 1))]
                                          (if (= -1 (.indexOf compare-value search-term)) false true))
	(.startsWith wildcard-value "*") (let [search-term (.substring wildcard-value 1 (.length wildcard-value))]
                                           (if (= -1 (.indexOf compare-value search-term)) false true))
	(.endsWith wildcard-value "*") (let [search-term (.substring wildcard-value 0 (- (.length wildcard-value) 1))]
                                         (if (= -1 (.indexOf compare-value search-term)) false true))
        :else (= wildcard-value compare-value))
    (= wildcard-value compare-value)))

(defn- not-compare-w [v c-v]
  (if (coll? v) (not (util/in? v c-v)) (not (wildcard-compare v c-v))))

(defn- not-in-compare [v c-v]
  (not (util/in? c-v v)))

(defn- in-compare [v c-v]
  (util/in? c-v v))

(defn- compare-w [v c-v]
  (if (coll? v) (util/in? v c-v) (wildcard-compare v c-v)))

(defn- compare-fn [v expression-map]
  (let [expression (if (map? expression-map) (first expression-map) (first {:value expression-map}))
        function-key (key expression)
        compare-value (val expression)
        compare-function (function-key {:not not-compare-w
					:not-in not-in-compare
                                        :in in-compare
				        :gt > 
					:lt <
					:gteq >=
					:lteq <=
                                        :value compare-w})]
    (compare-function v compare-value)))

(defn- filter-fn [criteria]
  (fn [map-entry]
    (if criteria
      (let [v (val map-entry)]
        (every? (fn [e] (compare-fn ((key e) v) (val e))) criteria))
      true)))

(defn create-memory-handler [data id-key]
  (let [index (atom 0)
        entity-map (atom (index-data data id-key index))]
    (reify PEntityHandler
      (handle-find-entity [_ id]
        (get @entity-map id))
      (handle-exist-entity [_ id]
	(if (get @entity-map id)
	  true
	  false))
      (handle-delete-entity [_ id]
	(when (get @entity-map id)
          (swap! entity-map dissoc id)
          id))
      (handle-update-entity [_ attributes]
	(when-let [id (id-key attributes)]
          (when-let [old (get @entity-map id)]
            (let [updated (merge old attributes)]
              (swap! entity-map assoc id updated)
              updated)))) 
      (handle-add-entity [_ attributes]
        (let [next-id (swap! index inc)
              attributes (assoc attributes id-key next-id)]
          (swap! entity-map assoc next-id attributes)
          attributes))
      (handle-list-entities [_ criteria sort-attrs from to]
	(into [] (map second (filter (filter-fn criteria) @entity-map)))) ; sorting and paging not implemented
      (handle-count-entities [_ criteria]
        (count (filter (filter-fn criteria) @entity-map)))
      (handle-get-unique-identifier [_]
	id-key))))
