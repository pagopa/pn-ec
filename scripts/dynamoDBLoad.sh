#!/bin/bash

while getopts 't:i:' opt ; do
  case "$opt" in 
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
      echo "Usage: $(basename $0) -t <table_name> -i <input_file_name>"
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

numOfLines=$(($(cat ${inputFileName} | wc -l)))
lineNum=0
while read line ;
do
  lineNum=$((++lineNum))
  echo -ne "$((lineNum*100/numOfLines))%\r"
  aws dynamodb put-item --table-name ${tableName} --item "${line}" > /dev/null
  rc=$?
  [[ $rc -ne 0 ]] && exit $rc
done < ${inputFileName}
