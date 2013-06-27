(defproject itedge/service-hub "1.1.0-SNAPSHOT"
  :plugins [[lein-sub "0.2.3"]]
  :sub ["core"
        "persistence-korma"
        "persistence-datomic"
        "http-ring"
        "http-pedestal"])