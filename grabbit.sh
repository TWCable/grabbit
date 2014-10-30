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
    json=`curl -X POST -H "Content-Type: application/json" -d "@$5" -u $3:$4 $2$INIT_URL`
    echo $json
elif [ $1 == $STATUS ]; then
    json=`curl -u $3:$4 --request GET $2$STATUS_URL"?jobIds="$5`
    echo $json
fi
