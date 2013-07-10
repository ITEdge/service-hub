(ns itedge.service-hub.core.util)

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn in? 
  "True if seq contains elm"
  {:added "EBS 1.0"}
  [seq elm]  
  (some #(= elm %) seq))

(defn update-map-values [m f]
  "Updates map values with provided fn"
  {:added "EBS 1.0"}
  (into {} (for [[k v] m] [k (f v)])))

(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  {:added "EBS 1.0"}
  [s]
  (if (re-find #"^-?\d+\.?\d*$" s)
    (read-string s)))

 (defn abs  
   "Returns absolute value of number"
   {:added "EBS 1.0"}
   [x]
   (if (< x 0) (- x) x)) 

(defn get-service-result
  "Create standard service result from supplied result-code and message"
  {:added "EBS 1.0"}
  [result-code message]
  {:return-code result-code :message message})

(defmacro pipeline-statements
  "Executes statements in given order one after another, the first which returns non-nil response
   will be returned (it's result), if every statement returns nil response, returns nil on the end."
  {:added "EBS 1.0"}
  ([fn]
    `(let [result# ~fn]
       result#))
  ([fn & fns]
    `(let [result# ~fn]
       (if (nil? result#) (pipeline-statements ~@fns) result#))))
