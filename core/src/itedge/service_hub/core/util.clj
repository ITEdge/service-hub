(ns itedge.service-hub.core.util)

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn in? 
  "True if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn wildcard-compare [compare-value wildcard-value]
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

(defn update-map-values
  "Updates map values with provided fn"
  [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (if (re-find #"^-?\d+\.?\d*$" s)
    (read-string s)))

 (defn abs  
   "Returns absolute value of number"
   [x]
   (if (< x 0) (- x) x)) 

(defn get-service-result
  "Create standard service result from supplied result-code and message"
  [result-code message]
  {:return-code result-code :message message})

(defmacro pipeline-statements
  "Executes statements in given order one after another, the first which returns non-nil response
   will be returned (it's result), if every statement returns nil response, returns nil on the end."
  ([fn]
    `(let [result# ~fn]
       result#))
  ([fn & fns]
    `(let [result# ~fn]
       (if (nil? result#) (pipeline-statements ~@fns) result#))))
