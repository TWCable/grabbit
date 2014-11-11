# Purpose #

The purpose of this project is to provide a reliable and fast solution for Copying content from a Source to Destination.
Source and destination can be any AEM 5.6.1 instances.
 
# Building #

## Prebuilt Grabbit package based off of latest master

[Grabbit.zip](https://s3.amazonaws.com/uploads.hipchat.com/34311/1348484/UtWP7P4iRlyd1s3/grabbit-1.0.1-SNAPSHOT.zip)

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

# Running Grabbit #

You can run Grabbit locally where your Client is local Author / Fresh Author and Server is local Publisher. It doesn't have to be this way ONLY, its just one option. Every CQ instance where this project is installed will have Client and Server parts installed.

Server exposes its functionality as a GET servlet at `/bin/twc/grab/server` which accepts a `path` as a `GET parameter`.

A path of the type `/a/b/.` indicates a non-recursive sync of all the sub-nodes for `b`

Client can ask for any `valid` content from server by hitting this servlet. The way this is done right now from Client is via OSGi Configs.

For eg: If Author is acting "Client", it can be configured to trigger a Sync by going to 
`/apps/grabbit/config/com.twc.grabbit.client.services.impl.DefaultClientService` in CRXDE Lite and configure the OSGi configs :

`grab.server.hostmane` : The Hostname of the Server

`grab.server.port` : The Port of the Server

`grab.server.username` : Username to connect to the Server

`grab.server.password` : Password to connect to the Server

Once these properties are set, `Save` them. Saving will kick of the Sync.

# Validating the Content Pull #

You can to `batch-client.log` on the Client and look for the message some thing like : 

```

Current Path : xxxx . Total nodes written : xxxx

...

Grab from localhost for Current Path xxxx took : xxxx milliseconds

```

Server monitoring is to go to `batch-server.log`.

*The current State of the Project is also deployed to `webcms-alpha02.lab.webapps.rr.com:4502` which can act as a Server.*


# Using the Grabbit Client UI#
**Grabbit Client UI is currently broken** 

Drop the component "Grabbit Client" under component group "TWC Grabbit". Enter comma separated path(s) in the textarea and verify the call to "/bin/twc/client/grab" in the Network tab.
A progress bar would appear on the top of the page indicating the content is being pulled in the form of a loading bar (red line).
Upon successful completion(status code 200), a modal pop would show "Content pulled successfully" , else "An error occurred while grabbing content." would be displayed.

You should also see UI notifications with status of the job that was kicked off. It will be of the following format 

```

Job Id : X

Path : XX

Start Time : XX

End Time : XX

Done? XX

ExitStatus : XX

Time Taken : XX ms

Number of Nodes : XX


```

The above status will be shown for every path that was requested.


# Initiating Grab/Sync Jobs via command line#

Following shell script (grabbit.sh) can be used to initiate grabbit jobs for one or more paths and checking the status of those jobs.

```shell
#!/bin/bash

# Simple Script to initialize one or more Sync or Grab jobs for one or more paths
# Sample usage :
# Initialize : grabbit.sh init http://localhost:4502 admin admin config.json
# Status : grabbit.sh status http://localhost:4502 admin admin <id1>,<id2>,..

#### Constants

INIT="init" #init => Initialize Sync for config file
STATUS="status" #status => Get Status for given JobIds delimited by commas
INIT_URL="/bin/twc/client/grab"
STATUS_URL="/bin/twc/client/grab/status"

if [ $1 == $INIT ]; then
    json=`curl -X POST -H "Content-Type: application/json" -d "@$5" -u $3:$3 $2$INIT_URL`
    echo $json
elif [ $1 == $STATUS ]; then
    json=`curl -u $3:$4 --request GET $2$STATUS_URL"?jobIds="$5`
    echo $json
fi
```

To use this script : 

`sh grabbit.sh init http://localhost:4502 admin admin config.json`

This script will return an array of one or more jobIds. You can use these jobIds to run a 'status' query on the same script
 
`sh grabbit.sh status http://localhost:4502 admin admin id1,id2,id3`

This status check returns a JSON representation of the same information you see in the Client UI

Here, `config.json` is the input file that contains configuration information for Grabbit.

### Config Format

```json
{
    "serverUsername" : "admin",
    "serverPassword" : "admin",
    "serverHost" : "localhost",
    "serverPort" : "4503",
    "pathConfigurations" :  [
        {
            "path" : "/content/residential-admin/ProductCatalog",
            "workflowConfigIds" : []
        },
        {
            "path" : "/content/dam/business",
            "workflowConfigIds" :
                [
                    "/etc/workflow/launcher/config/update_asset_mod",
                    "/etc/workflow/launcher/config/update_asset_create",
                    "/etc/workflow/launcher/config/dam_xmp_nested_writeback",
                    "/etc/workflow/launcher/config/dam_xmp_writeback"
                ]
        }
    ]
}
```
