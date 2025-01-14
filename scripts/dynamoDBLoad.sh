#!/bin/bash

aws_profile=""
aws_region="eu-south-1"

while getopts 't:i:p:r:e:' opt ; do
  case "$opt" in 
    p)
      aws_profile=${OPTARG}
    ;;
    r)
      aws_region=${OPTARG}
    ;;
    t)
      tableName=${OPTARG}
    ;;
    e)
      endpoint_url=${OPTARG}
    ;;
    i)
      inputFileName=${OPTARG}
    ;;
    :)
      echo -e "option requires an argument.\nUsage: $(basename $0) [-a] [-b] [-c arg]"
      exit 1
    ;;
    ?|h)
      echo "Usage: $(basename $0) -t <table_name> -i <input_file_name> [-p <aws_profile>] [-r <aws_region>] [-e <endpoint_url>]"
      exit 1
    ;;
  esac
done

if [[ ! $tableName || ! $inputFileName ]] ; then
  echo "both -t and -i parameters are mandatory"
  exit 1
fi

if [[ ! -f ${inputFileName} ]] ; then
  echo "invalid input file name ${inputFileName}"
  exit 1
fi

aws_command_base_args=""
if ( [ ! -z "${aws_profile}" ] ) then
  aws_command_base_args="${aws_command_base_args} --profile $aws_profile"
fi
if ( [ ! -z "${aws_region}" ] ) then
  aws_command_base_args="${aws_command_base_args} --region  $aws_region"
fi
if ( [ ! -z "${endpoint_url}" ] ) then
  aws_command_base_args="${aws_command_base_args} --endpoint-url  $endpoint_url"
fi
echo ${aws_command_base_args}

numOfLines=$(($(cat ${inputFileName} | wc -l)))
lineNum=0
pids=()
errorCount=0

while read line; do
  aws ${aws_command_base_args} dynamodb put-item --table-name ${tableName} --item "${line}" > /dev/null & \
  pids+=($!)
done < ${inputFileName}

for pid in "${pids[@]}"; do
  lineNum=$((lineNum + 1))
  wait $pid
  rc=$?
  if [[ $rc -ne 0 ]]; then
    errorCount=$((errorCount + 1))
    echo "Error on line $lineNum: AWS CLI command failed with exit code $rc. Line content: $line" >> $logFile
  else
    echo -ne "$((lineNum * 100 / numOfLines))%\r"
  fi

done

# Final summary
echo -e "\nProcessing complete."
echo "Total lines processed: $lineNum"
echo "Total errors: $errorCount"
if [[ $errorCount -gt 0 ]]; then
  echo "See $logFile for details on errors."
fi