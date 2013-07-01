(defproject itedge/service-hub "1.1.0"
  :plugins [[lein-sub "0.2.3"]]
  :sub ["core"
        "common"
        "persistence-korma"
        "persistence-datomic"
        "http-ring"
        "http-pedestal"])