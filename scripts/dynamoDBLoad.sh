#!/bin/bash

aws_profile=""
aws_region="eu-south-1"

while getopts 't:i:p:r:' opt ; do
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
    i)
      inputFileName=${OPTARG}
    ;;
    :)
      echo -e "option requires an argument.\nUsage: $(basename $0) [-a] [-b] [-c arg]"
      exit 1
    ;;
    ?|h)
      echo "Usage: $(basename $0) -t <table_name> -i <input_file_name> [-p <aws_profile>] [-r <aws_region>]"
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
echo ${aws_command_base_args}

numOfLines=$(($(cat ${inputFileName} | wc -l)))
lineNum=0
while read line ;
do
  lineNum=$((++lineNum))
  echo -ne "$((lineNum*100/numOfLines))%\r"
  aws ${aws_command_base_args} dynamodb put-item --table-name ${tableName} --item "${line}" > /dev/null
  rc=$?
  [[ $rc -ne 0 ]] && exit $rc
done < ${inputFileName}
