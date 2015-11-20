#!/bin/bash

# Simple Script to initialize one or more Sync or Grab jobs for one or more paths
# Sample usage :
# sh grabbit.sh
# Follow along to enter configuration information
# Follow along to get status of the jobs

#### Constants

GRABBIT_URL="/grabbit/job"

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
clear
`curl -s -X PUT --data-binary "@$configpath" -u $username:$password $client$GRABBIT_URL`
echo "~~~~~~~~~~~Jobs kicked off ~~~~~~~~~~~~~"
echo
echo "~~~~~Client configuration : $client $username $password Config: $configpath~~~~~"
# Monitor Job Status
while true; do
	echo	
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	read -p "Enter Job Id or \"all\" to get job status or enter 'q' to quit: " id
	if [ "$id" == "q" ]; then
		exit 0;
	fi
	statusJson=`curl -s -u $username:$password --request GET $client$GRABBIT_URL"/"$id.json`
	echo
	echo "Status for JobId [$id] is :"
	echo "$statusJson" | python -m json.tool 

done
