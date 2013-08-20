(ns itedge.service-hub.core.convertors-util
  (:require [itedge.service-hub.core.convertors :refer :all]
            [itedge.service-hub.core.util :as util]))

(defn- convert-value
  "Converts specified value, if value is a map, recursivly update all map values"
  {:added "EBS 1.0"}
  [convert-fn v]
  (if (map? v)
    (util/update-map-values v (partial convert-value convert-fn))
    (if v
      (convert-fn v)
      v)))

(defn- convert-map-values
  "Converts specified values in map by means of convert-fn"
  {:added "EBS 1.0"}
  [m keys-set convert-fn]
  (into {} (map (fn [item]
                  (let [k (key item)
                        v (val item)]
                    (hash-map k (if (keys-set k) (convert-value convert-fn v) v)))) m)))

(defn convert-specified-values
  "Converts specified values in map or vector of maps by means of convert-fn"
  {:added "EBS 1.0"}
  [vals keys-set convert-fn]
  (cond
    (nil? vals)
      vals
    (map? vals)
		  (convert-map-values vals keys-set convert-fn)
    (sequential? vals)
      (map #(convert-map-values % keys-set convert-fn) vals)
    :else (throw (Exception. (str "Only converting of maps and sequences of maps is supported")))))

(defn format-property
  "Returns function which formats property if property not nil"
  {:added "EBS 1.0"}
  [ft]
  (fn [property-value]
    (if property-value
      (format ft property-value)
      property-value)))

(defn sanitize-iso-params
  "Sanitize all whitespaces in params before conversion, should be + signs in case of ISO timestamps"
  {:added "EBS 1.0"}
  [convert-fn param]
  (convert-fn (.replaceAll param "\\s" "+")))

