#!/bin/bash

#Transfers 1 message from one SQS queue to another

if [[ $# -ne 2 ]]
then
    echo "usage $0 : from-queue to-queue"
    exit 1
fi

aws sqs receive-message --queue-url=$1 >m1
rc=$?
if [[ $rc -ne 0 ]] 
then
    echo error receiving msg - $rc
    exit $rc
fi
if [[ -s m1 ]]
then 
    cat m1 | jq ".Messages|.[0]|.Body|fromjson" > m1.body
    msgGroupId=`cat m1.body | jq -r ".digitalProgressStatusDto.generatedMessage.id"`
    rh=`cat m1 |  jq ".Messages|.[0]|.ReceiptHandle" |tr -d \"`
    aws sqs send-message --queue-url=$2 --message-body file://m1.body --message-group-id "$msgGroupId" > /dev/null
    rc=$?
    if [[ $rc -ne 0 ]] 
    then
        echo error sending msg - $rc
        exit $rc
    fi
    aws sqs delete-message --queue-url=$1  --receipt-handle=$rh
    rc=$?
    if [[ $rc -ne 0 ]] 
    then
        echo error deleting msg - $rc
        exit $rc
    fi
else 
    echo done
    exit 0
fi
