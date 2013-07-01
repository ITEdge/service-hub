(ns itedge.service-hub.core.test.authorizators-util
  (:use itedge.service-hub.core.authorizators-util
        itedge.service-hub.core.handlers
        clojure.test)
  (:require [itedge.service-hub.core.util :as util]))

(deftype TestEntityHandler []
  PEntityHandler
  (handle-find-entity [_ id]
    (if (= 1 id)
      {:id 1 :name "test-entity" :users [1 2 3]}
      nil))
  (handle-exist-entity [this id]
    (if (= 1 id)
      true
      false))
  (handle-delete-entity [this id]
    ) ; not implemented
  (handle-update-entity [this attributes]
    ) ; not implemented
  (handle-add-entity [this attributes]
    ) ; not implemented
  (handle-list-entities [_ criteria sort-attrs from to]
    [{:id 1 :name "test-entity" :users [1 2 3]}])
  (handle-count-entities [_ criteria]
    1)
  (handle-get-unique-identifier [_]
    :id))

(def test-entity-handler (->TestEntityHandler))

(deftest test-authenticated?
  (is (= (authenticated? {:id 1 :username "test" :roles #{:user :admin}}) nil))
  (is (= (authenticated? nil) 
         (util/get-service-result :not-authenticated "authentication is required for this service operation"))))

(deftest test-authorized?
  (is (= (authorized? {:id 1 :username "test" :roles #{:user :admin}} #{:user}) nil))
  (is (= (authorized? {:id 1 :username "test" :roles #{:user :admin}} #{:user :admin}) nil))
  (is (= (authorized? {:id 1 :username "test" :roles #{:user}} #{:admin})
         (util/get-service-result :not-authorized "user is not authorized for this service operation"))))

(deftest test-restrict-user-relations
  (is (= (restrict-user-relations {:id 1 :username "test" :roles #{:user :admin}} {:name "test-entity"} :users)
         {:name "test-entity" :users 1})))

(deftest test-entity-contains?
  (is (= (entity-contains? 1 1) true))
  (is (= (entity-contains? 1 2) false))
  (is (= (entity-contains? [1 2 3 4 5 6 7] 3) true))
  (is (= (entity-contains? [1 2 3] 7) false)))

(deftest test-user-relations-on-entity?
  (is (= (user-relations-on-entity? {:id 1 :username "test" :roles #{:user :admin}} 1 :users test-entity-handler) nil))
  (is (= (user-relations-on-entity? {:id 7 :username "test" :roles #{:user}} 1 :users test-entity-handler)
         (util/get-service-result :not-authorized "user is not authorized to operate on this entity"))))

(deftest test-contains-roles?
  (is (= (contains-roles? {:id 1 :username "test" :roles #{:user}} #{:user :admin}) true))
  (is (= (contains-roles? {:id 1 :username "test" :roles #{:user}} #{:admin}) false)))

