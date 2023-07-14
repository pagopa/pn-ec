#!/bin/bash

#Transfers all messages from one SQS queue to another using rerouteMsg.sh

if [[ $# -ne 2 ]]
then
    echo "usage $0 : from-queue to-queue"
    exit 1
fi

aws sqs get-queue-attributes --queue-url=$1 --attribute-name ApproximateNumberOfMessages > queue.attrs
rc=$?
if [[ $rc -ne 0 ]] 
then
    echo error getting queu attribute - $rc
    exit $rc
fi
numOfMsgs=`cat queue.attrs | jq -r ".Attributes.ApproximateNumberOfMessages"`
echo msgs to receive $numOfMsgs
rc=$?
for (( counter=1; counter<=$numOfMsgs; counter++ ))
do
  ./rerouteMsg.sh $1 $2
  rc=$?
  if [[ $rc -ne 0 ]] 
  then
      echo error rerouting msg - $rc
      exit $rc
  fi
  echo -ne "$((counter*100/numOfMsgs))%\r"
done

