#!/bin/bash

LOCALSTACK_ENDPOINT=http://localhost:4566
REGION=us-east-1
PROFILE=local
AWS_PROFILE="default"
ACCESS_KEY=TEST
SECRET_KEY=TEST
curr_dir=$(pwd)

log() { echo "[pn-ec-init-for-localdev][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }


dynamo_db_load() {
  log "### Populating DynamoDB ###"

  local BASE_REPO="https://raw.githubusercontent.com/pagopa/pn-ec/develop"
  local DYNAMO_SCRIPT_URL="$BASE_REPO/scripts/dynamoDBLoad.sh"
  local ANAGRAFICA_URL="$BASE_REPO/scripts/localdev/AnagraficaClient.json"
  local STATE_MACHINE_URL="$BASE_REPO/scripts/StateMachine.json"

  log "### Populating pn-EcAnagrafica ###" && \
  curl -sL "$DYNAMO_SCRIPT_URL" | bash -s -- \
    -t "pn-EcAnagrafica" \
    -i <(curl -sL "$ANAGRAFICA_URL") \
    -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-EcAnagrafica ###"; return 1; }

  log "### Populating pn-SmStates ###" && \
  curl -sL "$DYNAMO_SCRIPT_URL" | bash -s -- \
    -t "pn-SmStates" \
    -i <(curl -sL "$STATE_MACHINE_URL") \
    -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SmStates ###"; return 1; }
}

execute_init() {
  log "### Try to execute pn-ec init.sh ###"
  bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-ec/develop/src/test/resources/testcontainers/init.sh)
}

verify_localstack() {
  if ! curl -s $LOCALSTACK_ENDPOINT > /dev/null; then
    log "### Localstack is not running ###"
    exit 1
  fi
}

build_run() {
  local curr_dir=$(pwd)
  cd ..
  if ! ( ./mvnw -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=local -Daws.accessKeyId=TEST -Daws.secretAccessKey=TEST -Daws.region=us-east-1 -Daws.profile=default" spring-boot:run ); then
  echo "### Initialization failed ###"
  exit 1
  fi
  # Return to the original directory
  cd "$curr_dir" || return 1
}

main() {
  log "### Starting pn-ec ###"
  aws configure set cli_pager ""

  local start_time=$(date +%s)

  verify_localstack && \
  execute_init && \
  dynamo_db_load  && \
  build_run ||
  { log "### Failed to start pn-ec ###"; exit 1 }
  local end_time=$(date +%s)
  log "### pn-ec started ###"
  log "### Time taken: $((end_time - start_time)) seconds ###"
  cd "$curr_dir"
}

main