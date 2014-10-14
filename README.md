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

Current monitoring is to go to `batch-client.log` on the Client and look for the message some thing like : 

```

Current Path : xxxx . Total nodes written : xxxx

...

Grab from localhost for Current Path xxxx took : xxxx milliseconds

```

Server monitoring is to go to `batch-server.log`.

*The current State of the Project is also deployed to `webcms-alpha02.lab.webapps.rr.com:4502` which can act as a Server.*


# Accessing the client UI#

Drop the component "Sync service component" under component group "grabbit". Enter comma separated path(s) in the textarea and verify the call to "/bin/grabbit/client/pull" in the Network tab.
A progress bar would appear on the top of the page indicating the content is being pulled in the form of a loading bar (red line).
Upon successful completion(status code 200), a modal pop would show "Content pulled successfully" , else "An error occurred while grabbing content." would be displayed.

# Initiating Grab/Sync Jobs via command line#

Following shell script (grabbit.sh) can be used to initiate grabbit jobs for one or more paths and checking the status of those jobs.

```shell
#!/bin/bash

# Simple Script to initialize one or more Sync or Grab jobs for one or more paths

#### Constants

INIT="init" #init => Initialize Sync for given paths delimited by commas
STATUS="status" #status => Get Status for given JobIds delimited by commas
HOST="http://localhost:4502"
INIT_URL="/bin/twc/client/grab"
STATUS_URL="/bin/twc/client/grab/status"

if [ $1 == $INIT ]; then
    curl -u admin:admin --request GET $HOST$INIT_URL"?paths="$2
elif [ $1 == $STATUS ]; then
    curl -u admin:admin --request GET $HOST$STATUS_URL"?jobIds="$2
fi
```

To use this script : 

`sh grabbit.sh init /etc/tags,/content/residential-admin,/content/twc/en/checkout`

This script will return an array of one or more jobIds. You can use these jobIds to run a 'status' query on the same script
 
`sh grabbit.sh status id1,id2,id3`

This status check returns a JSON representation of the same information you see in the Client UI