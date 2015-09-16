# RELEASE NOTES

## 3.0.4

* Finished implementing "delta copy" functionality

## 3.0.3

* Adding DEBUG logging for nodes being saved on Client

## 3.0.2

* Fixed bug that crept in around deleteBeforeWrite

## 3.0.1

* Updated Client and Server to use request credentials as login credentials when creating a JCR session (GH-56)

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
