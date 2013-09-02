# Service-Hub Changelog

## 1.3.0 - September 2, 2013

* Changed signature of the method validate-list-entities in PEntityServiceValidator, now it takes also sort-args for possible validation
* Introduced new functionality in services-util, the function scaffold-service, which yields instance of the type implementing the
  PEntityService protocol, given the mandatory handler (of the type PEntityHandler) and optional validator (PEntityServiceValidator) and 
  authorizator (PEntityServiceAuthorizator) arguments, this should greatly reduce the effort of creating simple services
* Introduced datomic support in sub-project persistence-datomic	
