#!/bin/bash

aws_profile=""
aws_region="us-east-1"

while getopts 't:i:p:r:e:j:' opt ; do
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
    j)
      MAX_PARALLEL=${OPTARG}
    ;;
    :)
      echo -e "option requires an argument.\nUsage: $(basename $0) [-a] [-b] [-c arg]"
      exit 1
    ;;
    ?|h)
      echo "Usage: $(basename $0) -t <table_name> -i <input_file_name> [-p <aws_profile>] [-r <aws_region>] [-e <endpoint_url>] [-j <max_parallel>]"
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

numOfLines=$(grep -c '^' "${inputFileName}")
lineNum=0
pids=()
errorCount=0
MAX_PARALLEL=${MAX_PARALLEL:-1}

running=0

while IFS= read -r line || [[ -n "$line" ]]; do
  # Esegui il comando in background
  aws ${aws_command_base_args} dynamodb put-item --table-name "${tableName}" --item "${line}" > /dev/null &
  pid=$!
  pids+=($pid)
  linesMap[$pid]="$line"
  ((running++))
  lineNum=$((lineNum + 1))
  echo -ne "$((lineNum * 100 / numOfLines))%\r"

  # Se abbiamo raggiunto il massimo, attendi che uno termini
  if [[ $running -ge $MAX_PARALLEL ]]; then
    wait -n
    ((running--))
  fi
done < "${inputFileName}"

# Attendi il completamento di tutti i processi restanti
for pid in "${pids[@]}"; do
  wait "$pid"
  rc=$?
  if [[ $rc -ne 0 ]]; then
    errorCount=$((errorCount + 1))
    echo "Error on line $lineNum: AWS CLI command failed with exit code $rc. Line content: ${linesMap[$pid]}" >> "$logFile"
  fi
done


# Final summary
echo -e "\nProcessing complete."
echo "Total lines processed: $lineNum"
echo "Total errors: $errorCount"
if [[ $errorCount -gt 0 ]]; then
  echo "See $logFile for details on errors."
fi