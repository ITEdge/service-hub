(ns itedge.service-hub.core.handlers-memory
  (:require [itedge.service-hub.core.handlers :refer :all]
            [itedge.service-hub.core.util :as util]))

(defn- index-data [data id-key index]
  (reduce (fn [acc item]
            (let [next-id (swap! index inc)]
              (assoc acc next-id (assoc item id-key next-id)))) {} data))

(defn- compare-fn [v expression-map]
  (let [expression (if (map? expression-map) (first expression-map) (first {:value expression-map}))
        function-key (key expression)
        compare-value (val expression)
        compare-function (function-key {:not (fn [v c-v] (if (coll? v) (not (util/in? v c-v)) (not= v c-v)))
										                    :not-in (fn [v c-v] (not (util/in? c-v v)))
                                        :in (fn [v c-v] (util/in? c-v v))
										                    :gt > 
										                    :lt <
										                    :gteq >=
										                    :lteq <=
                                        :value (fn [v c-v] (if (coll? v) (util/in? v c-v) (= v c-v)))})]
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
