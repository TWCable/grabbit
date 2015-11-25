#!/bin/bash
set -o pipefail

SET_NO_COLOR=$(tput sgr0)
SET_BLUE=$(tput setaf 4)
SET_GREEN=$(tput setaf 2)
SET_YELLOW=$(tput setaf 3)
SET_RED=$(tput setaf 1)

# Simple Script to initialize one or more Sync or Grab jobs for one or more paths
# Sample usage :
# sh grabbit.sh
# Follow along to enter configuration information
# Follow along to get status of the jobs

#### Constants

GRABBIT_URL="/grabbit/job"

clear
echo $SET_BLUE
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~GRABBIT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo $SET_NO_COLOR

echo "Enter Client [http(s)://server:port] to initiate Grabbit request "
read client

echo
echo "Client Username :"
read -s username 
echo
echo "Client Password :"
read -s password 

echo
echo "Enter path to your Grabbit Configuration file"
read configpath

rm -f /tmp/grabbit
# Kick off Grabbit jobs
clear
echo $SET_YELLOW
echo "Processing....."
echo $SET_NO_COLOR
curl -s -f -X PUT --data-binary "@$configpath" -u $username:$password $client$GRABBIT_URL > /tmp/grabbit
if [ $? -eq 22 ]; then
    clear
    echo $SET_RED
    echo "~~~~~~~~~Failure~~~~~~~~~~~~~"
    echo   
    echo "Something went wrong when processing the given config. Please check the client log for more details."
    echo    
    exit 1
fi
clear
echo "~~~~~~~~~~~Jobs kicked off ~~~~~~~~~~~~~"
echo 
echo "Job IDs: ${SET_GREEN}$(cat /tmp/grabbit |  sed -e 's/\]//' -e 's/\[//' -e 's/,/  /g')${SET_NO_COLOR}"
echo 
echo "~~~~~Client configuration : Client : $client Config: $configpath ~~~~~"
# Monitor Job Status
while true; do
	echo	
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	read -p "Enter Job ID or \"all\" to get job status or enter 'q' to quit: " id
	if [ "$id" == "q" ]; then
		exit 0;
	fi
	statusJson=`curl -s -u $username:$password --request GET $client$GRABBIT_URL"/"$id.json`
	echo
	echo "Status for JobId [$id] is :"
	echo "$statusJson" | python -m json.tool 

done
