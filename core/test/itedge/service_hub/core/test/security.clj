(ns itedge.service-hub.core.test.security
  (:require [itedge.service-hub.core.security :refer :all]
            [clojure.test :refer :all]))

(deftest test-bcrypt-credential-fn
  (let [cred-fn (fn [username] 
	          {:username username 
                   :password "$2a$10$ZGsZxKHCvDsXLB2dCuMHcumKAmwQ4sh2YIH4k5tGifXDrlKdddbXi"})]
	  (is (= (bcrypt-credential-fn cred-fn {:username "test" :password "admin"}) {:username "test"}))
	  (is (= (bcrypt-credential-fn cred-fn {:username "test" :password "user"}) nil))))

(deftest test-authorized?
  (is (= (authorized? #{:admin :user} {:username "test" :roles #{:admin}}) :admin))
  (is (= (authorized? #{:admin} {:username "test" :roles #{:user}}) nil)))
