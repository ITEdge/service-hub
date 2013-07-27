(defproject itedge/service-hub.persistence-datomic "1.1.0"
  :description "Datomic persistence layer"
  :min-lein-version "2.0.0"
  :url "https://github.com/ITEdge/ServiceHub"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [itedge/service-hub.core "1.1.0"]
                 [com.datomic/datomic-free "0.8.4020.26"]])