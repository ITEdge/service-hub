(ns itedge.service-hub.http-ring.content-util
  (:require [ring.util.response :refer :all]
            [clojure.data.json :as js]))

(defn craft-json-response
  "Creates json ring response map, jsonize first argument and adds content type"
  {:added "EBS 1.0"}
  [item]
  (-> (response (js/write-str item))
      (content-type "application/json")))

(defn read-json
  "Read json data, converts map keys to clojure keywords"
  {:added "EBS 1.0"}
  [data]
  (js/read-str data :key-fn keyword))

(defn craft-edn-response
  "Creates edn ring response map, serializes first argument into string and adds content type"
  {:added "EBS 1.0"}
  [item]
  (-> (response (pr-str item))
      (content-type "application/edn")))