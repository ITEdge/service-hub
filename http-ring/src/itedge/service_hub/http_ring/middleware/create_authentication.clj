(ns itedge.service-hub.http-ring.middleware.create-authentication
  (:import org.apache.commons.codec.binary.Base64))

(defn- create-authentication
  "Creates authentication from http basic header security token in request and assoc auth info in request for future use"
  {:added "EBS 1.0"}
  [{{:strs [authorization]} :headers :as request} credential-fn]
  (if authorization
	  (if-let [[[_ username password]] (try (-> (re-matches #"\s*Basic\s+(.+)" authorization)
	                                          second
	                                          (.getBytes "UTF-8")
	                                          Base64/decodeBase64
	                                          (String. "UTF-8")
	                                          (#(re-seq #"([^:]+):(.*)" %)))
	                                     (catch Exception e
	                                       ; could let this bubble up and have an error page take over,
	                                       ;   but basic is going to be used predominantly for API usage, so...
	                                       ; TODO should figure out logging for widely-used library; just use tools.logging?
	                                       (println "Invalid Authorization header for HTTP Basic auth: " authorization)
	                                       ;(.printStackTrace e)
                                        ))]
	    (if-let [auth (credential-fn {:username username, :password password})]
	      (assoc request :auth auth)
	      (assoc request :auth :bad-credentials))
	    (assoc request :auth :malformed-credentials))    
      (assoc request :auth :no-credentials)))

(defn wrap-create-authentication
  "Middleware that creates authentication info from http basic header security token"
  [handler credential-fn]
  (fn [req]
    (handler (create-authentication req credential-fn))))

