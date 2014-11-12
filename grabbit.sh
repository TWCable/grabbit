#!/bin/bash

# Simple Script to initialize one or more Sync or Grab jobs for one or more paths
# Sample usage :
# sh grabbit.sh
# Follow along to enter configuration information
# Follow along to get status of the jobs

#### Constants

INIT_URL="/bin/twc/client/grab"
STATUS_URL="/bin/twc/client/grab/status"

echo "Enter Client [http(s)://server:port] to initiate Grabbit request "
read client

echo
echo "Client Username :"
read username
echo
echo "Client Password :"
read password

echo
echo "Enter path to your Grabbit Configuration file"
read configpath

# Kick off Grabbit jobs
initResponse=`curl -X POST -H "Content-Type: application/json" -d "@$configpath" -u $username:$password $client$INIT_URL`
clear
echo "~~~~~Client configuration : $client $username $password Config: $configpath~~~~~"

# Monitor Job Status
while true; do
	echo
	echo "~~~~~~~~~~~Jobs kicked off : $initResponse ~~~~~~~~~~~~~"
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	read -p "Enter Job Id(s) to get job status or enter 'q' to quit: " ids
	if [ "$ids" == "q" ]; then
		exit 0;
	fi
	statusJson=`curl -u $username:$password --request GET $client$STATUS_URL"?jobIds="$ids`
	echo
	echo "Status for JobIds [$ids] is :"
	echo "$statusJson" | python -m json.tool 

done
