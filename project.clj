(defproject itedge/service-hub "1.3.0"
  :plugins [[lein-sub "0.2.4"]]
  :sub ["core"
        "common"
        "persistence-korma"
        "persistence-datomic" 
        "http-ring" ])
