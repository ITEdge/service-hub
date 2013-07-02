(ns itedge.service-hub.http-ring.middleware.test.create-authentication
  (:use itedge.service-hub.http-ring.middleware.create-authentication
        clojure.test))

(deftest test-wrap-create-authentication
  (let [credential-fn (fn [{username :username password :password :as auth}] 
                        (when (and (= username "Aladdin") (= password "open sesame"))
                          (assoc auth :id 1)))
        wrapped-handler (wrap-create-authentication identity credential-fn)
        request-good {:headers {"authorization" "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="}}
        request-no-creds {:headers {}}
        request-malformed-creds {:headers {"authorization" "Blaaah"}}
        request-bad-creds {:headers {"authorization" "Basic amhlOnVzZXI="}}]
    (is (map? (:auth (wrapped-handler request-good))))
    (is (= (:auth (wrapped-handler request-no-creds)) :no-credentials))
    (is (= (:auth (wrapped-handler request-malformed-creds)) :malformed-credentials))
    (is (= (:auth (wrapped-handler request-bad-creds)) :bad-credentials))))