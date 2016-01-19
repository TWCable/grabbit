# RELEASE NOTES

## 4.1.1

* Fixed concurrency issues with the Workflow Manager. Miscellaneous Workflow Manager refactoring
* Trivial README updates

## 4.1.0

* Job paths are now validated as to not write 'dirty data'
* Small performance improvements
* Improved shell
* Small fixes

## 4.0.7

* Added YAML support. Backwards compatible to existing JSON format
* Use AuthCache for Apache HttpClient to do preemptive authentication

## 4.0.6

* Update Grabbit 4.0.x build so that OOTB/Provided dependencies are properly excluded

## 4.0.5

* Updated README to remove instructions around workflow bundle
* Use earlier version of AEM workflow so that CI jobs can complete. Issue #63
* Removed some unnecessary files : karama.gradle, reporting.gradle
* Clarified versioning and compatibility in README
* Small changes to README

## 4.0.4

* Finished implementing "delta copy" functionality

## 4.0.3

* Adding DEBUG logging for nodes being saved on Client

## 4.0.2

* Bug fixes

## 4.0.1

* Updated Client and Server to use request credentials as login credentials when creating a JCR session (GH-56)

## 4.0.0

* Updated for AEM 6.1

## 3.0.0

* Added "excludePath" feature ( GH-23 )
* Added "Delete before write" functionality ( GH-20 )
* Fixed issue with not writing mandatory child nodes with their parent ( GH-34 )
* A number of smaller bug fixes and refactoring

## 2.0.1

* Removed an unnecessary bundle dependency on SCR and downgraded guava to 15 to make AEM6 happy
* Removed the broken client UI code (#13)

## 2.0.0

Initial public release
