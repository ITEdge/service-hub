(ns itedge.service-hub.http-ring.middleware.translate-params
  (:require [clojure.string :as string]
            [itedge.service-hub.core.util :as util]))

(defn- translate-params [target]
  (cond
    (map? target)
      (into {}
        (for [[k v] target]
          [k (translate-params v)]))
    (vector? target)
      (vec (map translate-params target))
    :else
      (let [v (get {"null" nil "true" true "false" false} target target)]
        (if (and (string? v) (= \[ (first v)) (= \] (last v)))
          (into [] (map util/parse-number (string/split (.replaceAll v "\\[|\\]" "") #",")))
          v))))

(defn wrap-translate-params
  "Middleware that converts the null, true and false values to clojure equivalents - nil, true and false"
  [handler]
  (fn [req]
    (handler (update-in req [:params] translate-params))))

