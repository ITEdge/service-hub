# Service-Hub Changelog

## 1.3.1 - September 6, 2013

* Added aditional boolean flag parameter 'parse-numeric?' to methods get-route, update-route and scaffols-crud-routes in namespace
  itedge.service-hub.http-ring.routes-util to indicate if id from url should be parsed as number or not
* Updated method add-entity in itedge.service-hub.persistence-datomic.util to use correct way to resolve temporary IDs

## 1.3.0 - September 2, 2013

* Changed signature of the method validate-list-entities in PEntityServiceValidator, now it takes also sort-args for possible validation
* Introduced new functionality in services-util, the function scaffold-service, which yields instance of the type implementing the
  PEntityService protocol, given the mandatory handler (of the type PEntityHandler) and optional validator (PEntityServiceValidator) and 
  authorizator (PEntityServiceAuthorizator) arguments, this should greatly reduce the effort of creating simple services
* Introduced datomic support in sub-project persistence-datomic	
