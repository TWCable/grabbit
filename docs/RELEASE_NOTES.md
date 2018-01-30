# RELEASE NOTES

[ ![Download](https://api.bintray.com/packages/twcable/aem/Grabbit/images/download.svg) ](https://bintray.com/twcable/aem/Grabbit/_latestVersion)

## 7.1.3

* Bug fixes (Fixes #192)

## 7.1.2

* Bug fixes

## 7.1.1
* Bug fixes
* Documentation changes
* Log names are now changed from batch-client.log to grabbit-receive.log; and batch-server.log to grabbit-send.log

## 7.1.0
* Support for writing rep:policy nodes
* Bug fixes
* Performance improvements

## 7.0.2
* Bug fixes

## 7.0.1
* Add https sync support (fix #149)

## 7.0.0

* Refactor JcrNodesProcessor to use JcrPropertyDecorator
* Provide Root ResourceProvider for Grabbit
* Upgrade joda-time dependency to 2.7 and update MANIFEST.MF to accept [2,3) for AEM 6.1 & AEM 6.2 compatibility
* Added Content-Type header in curl request in grabbit.sh for it to work in AEM 6.2
* Transfer current property's state whether it's multiple or not from Server to Client

## 5.0.1

* Adds feature that provides a way to delete JcrJobRepository that is older than X hours from "now"
* The API is : POST /grabbit/jobrepository/clean --data hours=X

## 5.0.0

* Added transaction support
* New resource modeling, with new RESTful endpoints
* Some misc refactoring
* Additional specifications for missing code coverage

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
