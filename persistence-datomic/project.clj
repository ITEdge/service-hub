(defproject itedge/service-hub.persistence-datomic "1.3.0-SNAPSHOT"
  :description "Datomic persistence layer"
  :min-lein-version "2.0.0"
  :url "https://github.com/ITEdge/service-hub"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [itedge/service-hub.core "1.2.1"]
                 [com.datomic/datomic-free "0.8.4138"]])
