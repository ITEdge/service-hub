(defproject itedge/service-hub.http-ring "1.1.0"
  :description "Service-Hub http ring/compojure layer"
  :min-lein-version "2.0.0"
  :url "https://github.com/ITEdge/ServiceHub"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.0"]
                 [itedge/service-hub.core "1.1.0"]
                 [ring "1.1.8"]
                 [compojure "1.1.3"]
                 ;java dependencies
                 [commons-codec "1.6"]])
