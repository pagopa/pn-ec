#!/bin/bash

curr_dir=$(pwd)
echo "Starting pn-ec..."

if ! ../src/test/resources/testcontainers/init.sh; then
  echo "### Failed to run init.sh ###"
  exit 1
fi

if ! ./dynamoDBLoad.sh -t pn-EcAnagrafica -i AnagraficaClient.json -e http://localhost:4566; then
  echo "### Failed to populate pn-EcAnagrafica table ###"
  exit 1
fi

cd ..

if ! ( ./mvnw -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=local -Daws.accessKeyId=TEST -Daws.secretAccessKey=TEST -Daws.region=eu-south-1" spring-boot:run ); then
  echo "### Initialization failed ###"
  exit 1
fi

# Return to the original directory
cd "$curr_dir"