(defproject itedge/service-hub "1.3.4-SNAPSHOT"
  :description "Rapid development framework"
  :url "https://github.com/ITEdge/service-hub"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-sub "0.2.4"]]
  :sub ["core"
        "common"
        "persistence-korma"
        "persistence-datomic"
        "http-ring" ]
  :deploy-repositories [["releases" :clojars]])
