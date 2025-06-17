#!/bin/bash

curr_dir=$(pwd)
echo "Starting pn-ec..."

if ! ./dynamoDBLoad.sh -t pn-EcAnagrafica -i AnagraficaClient.json -e http://localhost:4566; then
  echo "### Failed to populate pn-EcAnagrafica table ###"
  exit 1
fi

if ! ./dynamoDBLoad.sh -t pn-SmStates -i StateMachines.json -e http://localhost:4566; then
  echo "### Failed to populate pn-SmStates table ###"
  exit 1
fi

cd ..

if ! ( ./mvnw -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=local -Daws.accessKeyId=TEST -Daws.secretAccessKey=TEST -Daws.region=eu-south-1" spring-boot:run ); then
  echo "### Initialization failed ###"
  exit 1
fi

# Return to the original directory
cd "$curr_dir"