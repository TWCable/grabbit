# Purpose #

The purpose of this project is to provide a reliable and fast solution for Copying content from a Source to Destination.
Source and destination can be any AEM 5.6.1 instances.
 
# Building #

## Full Clean Build ##

`gradlew clean install` 

## Pre-requisites ##

### Installing Protocol Buffers Compiler ###

#### For Windows ####

Install the binary from https://code.google.com/p/protobuf/downloads/detail?name=protoc-2.4.1-win32.zip&can=2&q= and then set the `/path/to/protoc.exe` on your PATH variable.

#### For Macs ####

```

brew tap homebrew/versions

brew install protobuf241

brew link --force --overwrite protobuf241

```

_For both Windows and Mac : To verify that installation was successful, `protoc --version` should display `2.4.1`_

# Running the Content Sync #

You can run content Sync locally where your Client is local Author / Fresh Author and Server is local Publisher. It doesn't have to be this way ONLY, its just one option. Every CQ instance where this project is installed will have Client and Server parts installed.

Server exposes its functionality as a GET servlet at `/bin/twc/sync/server` which accepts a `rootPath` as a `GET parameter`.

Client can ask for any `valid` content from server by hitting this servlet. The way this is done right now from Client is via OSGi Configs.

For eg: If Author is acting "Client", it can be configured to trigger a Sync by going to 
`/apps/poc/config/com.twc.webcms.sync.client.services.impl.SyncClientServiceImpl` in CRXDE Lite and configure the OSGi configs :

`sync.paths` : The `paths` to sync
A path of the type `/a/b/.` indicates a non-recursive sync of all the sub-nodes for `b`

`sync.server.hostmane` : The Hostname of the Server

`sync.server.port` : The Port of the Server

`sync.server.username` : Username to connect to the Server

`sync.server.password` : Password to connect to the Server

`sync.enabled` : Setting this to TRUE will KICK OFF a request for Content Sync from Server -> Client.

Once these properties are set, `Save` them. Saving will kick of the Sync.

# Validating the Content Sync #

Currently, there is no UI component to this that will give any 'notifications' of the status of the sync.

Current monitoring is to go to `sync-client.log` on the CLient and look for the message some thing like : 

```

Received all data from Server for Sync. Completed unmarshalling. Total nodes : XXXX

...

'Content sync from <server> for Content Path <rootPath>': running time (millis) = xxxxx

```

*The current State of the Project is also deployed to `webcms-alpha02.lab.webapps.rr.com:4502` which can act as a Server.*
