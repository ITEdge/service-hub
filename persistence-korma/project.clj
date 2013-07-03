(defproject itedge/service-hub.persistence-korma "1.1.0"
  :description "Service-Hub korma persistence layer (SQL abstraction layer)"
  :min-lein-version "2.0.0"
  :url "https://github.com/ITEdge/ServiceHub"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [itedge/service-hub.core "1.1.0"]
                 [korma-enhanced "0.3.1"]])