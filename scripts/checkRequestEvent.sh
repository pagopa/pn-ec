#!/bin/bash

region=eu-south-1
account=089813480515

while getopts 'a:i:r:p:' opt ; do
  case "$opt" in 
    r)
      region=${OPTARG}
    ;;
    a)
      account=${OPTARG}
    ;;
    i)
      inputFileName=${OPTARG}
    ;;
    p)
      profile=${OPTARG}
    ;;
    :)
      >&2 echo -e "option requires an argument.\nUsage: $(basename $0) -i <input_file_name> -p <aws_profile> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
    ?|h)
      >&2 echo "Usage: $(basename $0) -i <input_file_name> -p <aws_profile> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $inputFileName ]] ; then
  >&2 echo "-i parameter is mandatory"
  exit 1
fi

if [[ ! $profile ]] ; then
  >&2 echo "-p parameter is mandatory"
  exit 1
fi

if [[ ! -f ${inputFileName} ]] ; then
  <&2 echo "invalid input file name ${inputFileName}"
  exit 1
fi

echo "{ \"#K\": \"documentKey\",\"#S\": \"documentState\"}" > projection.json
          
>&2 echo -e "Questa versione esegue gli step seguenti:\n\
  1. per tutti gli eventi presenti nel file di input (prelevati da una delle code del pn-ec-tracker-*) viene controllato se tale evento è già stato registrato nella event list della richiesta\n\
Se non sono specificati i parametri opzionali vengono usati i parametri dell'ambiente Dev"
  

jq -r '.xpagopaExtchCxId + "~" + .requestIdx + " " + .nextStatus + " " + .digitalProgressStatusDto.eventTimestamp + " " + .digitalProgressStatusDto.generatedMessage.system + " " + .digitalProgressStatusDto.generatedMessage.id + " " + .digitalProgressStatusDto.generatedMessage.location' ${inputFileName} \
| while read -r requestKey nextStatus timestamp system id location; do
  
  response=$(aws dynamodb get-item --table-name pn-EcRichiesteMetadati --key "{\"requestId\": {\"S\": \"${requestKey}\"}}" )
  if [ "x${response}" == "x" ] ; then
    echo "[ERROR] ${requestKey} - Not found"
    continue
  fi
  query=".Item.eventsList.L[].M.digProgrStatus.M | .status.S == \"${nextStatus}\""
  found=$(echo ${response} | jq "${query}" | grep true)
  if [ "x${found}" == "xtrue" ] ; then
    query=".Item.eventsList.L[].M.digProgrStatus.M | .status.S == \"${nextStatus}\" and .eventTimestamp.S == \"${timestamp}\" and .generatedMessage.M.system.S == \"${system}\" and .generatedMessage.M.id.S == \"${id}\" and .generatedMessage.M.location.S == \"${location}\""
    found=$(echo ${response} | jq "${query}" | grep true)
    if [ "x${found}" == "xtrue" ] ; then
      echo "[INFO ] ${requestKey} - event \"${nextStatus}\" duplicated - Ok"
    else
      echo "[ERROR] ${requestKey} - event \"${nextStatus}\" duplicated but attributes mismatch - Warning"
    fi
  else
    echo "[ERROR] ${requestKey} - event \"${nextStatus}\" MISSING - KO"
  fi
done
