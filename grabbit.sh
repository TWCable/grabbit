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

GRABBIT_JOB="/grabbit/job"
GRABBIT_TRANSACTION="/grabbit/transaction"


rm -f /tmp/grabbit
rm -f /tmp/grabbit_headers

function newGrabbitRequest() {
    echo
    echo "Enter path to your Grabbit Configuration file"
    read configpath
    clear
    echo $SET_YELLOW
    echo "Processing....."
    echo $SET_NO_COLOR
    curl -s -f -X PUT --header "Content-Type: text/plain" --data-binary "@$configpath" -u $username:$password -D /tmp/grabbit_headers $client$GRABBIT_JOB > /tmp/grabbit
    RSP_CODE=$?
    if [ $RSP_CODE -ne 0 ]; then
        clear
        echo $SET_RED
        echo "~~~~~~~~~Failure~~~~~~~~~~~~~"
        echo
        echo "Something went wrong when processing the given config."
        echo
        echo "cURL, the transfer library used by this shell received an error code of $RSP_CODE. See https://curl.haxx.se/libcurl/c/libcurl-errors.html for debugging."
        echo "If this doesn't help, please check the client log for more details."
        exit 1
    fi
    clear
    echo "~~~~~~~~~~~Jobs kicked off ~~~~~~~~~~~~~"
    echo
    echo "Transaction ID: ${SET_BLUE}$(cat /tmp/grabbit_headers | grep GRABBIT_TRANSACTION_ID | sed -e 's/GRABBIT_TRANSACTION_ID: //')${SET_NO_COLOR}"
    echo "Job IDs: ${SET_GREEN}$(cat /tmp/grabbit |  sed -e 's/\]//' -e 's/\[//' -e 's/,/  /g')${SET_NO_COLOR}"
    echo
    echo "~~~~~Client configuration : Client : $client Config: $configpath ~~~~~"
}

function monitorStatus {
    while true; do
        echo
        echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        echo "Enter \"j\" for job status, \"t\" for transaction status, or \"q\" to quit"
        read selection
        if [ "$selection" == "q" ]; then
            exit 0;
        elif [ "$selection" == "j" ]; then
            while true; do
                echo "Enter jobID to access status of a job, \"all\" to access all jobs, or \"b\" to go back"
                read selection
                if [ "$selection" == "b" ]; then
                   break
                fi
                statusJson=`curl -s -u $username:$password --request GET $client$GRABBIT_JOB"/"$selection.json`
                echo
                echo "Status for JobId [$selection] is :"
                echo "$statusJson" | python -m json.tool
            done
        elif [ "$selection" == "t" ]; then
            echo "Enter transactionID to access transaction status, or \"b\" to go back"
            read selection
            if [ "$selection" == "b" ]; then
                break
            fi
            statusJson=`curl -s -u $username:$password --request GET $client$GRABBIT_TRANSACTION"/"$selection.json`
            echo
            echo "Status for transaction [$selection] is :"
            echo "$statusJson" | python -m json.tool
        else
            echo "Invalid selection"
        fi
    done
}

function stopJob {
    echo "Enter jobID to stop, or \"b\" to go back"
    read selection
    if [ "$selection" == "b" ]; then
    echo "*****************************************************"
       return
    fi
    statusJson=`curl -s -u $username:$password --request DELETE $client$GRABBIT_JOB"?jobId="$selection`
    echo
    echo "$statusJson"
}


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

while true; do
    echo "Enter \"n\" for new job request, \"m\" to monitor, \"s\" to stop a job, or \"q\" to quit"
    read selection
    if [ "$selection" == "n" ]; then
        newGrabbitRequest
    elif [ "$selection" == "m" ]; then
        monitorStatus
    elif [ "$selection" == "s" ]; then
        stopJob
    elif [ "$selection" == "q" ]; then
        exit 0;
    else
        echo "Invalid selection"
    fi
done
