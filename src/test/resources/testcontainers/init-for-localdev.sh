#!/bin/bash
COMMIT_ID=${1:-develop}
set -euo pipefail

# Configurazione
VERBOSE=false
LOCALSTACK_ENDPOINT=http://localhost:4566
REGION=us-east-1
AWS_PROFILE="default"
ACCESS_KEY=TEST
SECRET_KEY=TEST
CLI_PAGER=$(aws configure get cli_pager)

# Logging
log() { echo "[pn-ec-init][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }
silent() { "$@" > /dev/null 2>&1 || true; }

# Verifica LocalStack
verify_localstack() {
  curl -fs "$LOCALSTACK_ENDPOINT" > /dev/null || {
    log "LocalStack non è attivo"; exit 1;
  }
}

# Popolamento DynamoDB
populate_table() {
  local table=$1 url=$2
  log "Populating $table"
  tmpfile=$(mktemp)
  curl -sL "$url" > "$tmpfile"
  curl -sL "https://raw.githubusercontent.com/pagopa/pn-ec/$COMMIT_ID/scripts/dynamoDBLoad.sh" | \
    bash -s -- -t "$table" -i "$tmpfile" -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
    log "Failed to populate $table"
}

load_dynamodb() {
  log "Populating DynamoDB"
  local base="https://raw.githubusercontent.com/pagopa/pn-ec/$COMMIT_ID/scripts"
  populate_table "pn-EcAnagrafica" "$base/AnagraficaClient.json"
  populate_table "pn-SmStates" "$base/StateMachines.json"
}

# Inizializzazione
execute_init() {
  log "Executing init script"
  bash <(curl -s "https://raw.githubusercontent.com/pagopa/pn-ec/$COMMIT_ID/src/test/resources/testcontainers/init.sh")
}

# Main
main() {
  log "Starting pn-ec localdev configuration."
  aws configure set cli_pager ""
  local start=$(date +%s)

  verify_localstack
  execute_init
  load_dynamodb

  local duration=$(( $(date +%s) - start ))
  log "init-for-localdev.sh executed in ${duration}s"
  aws configure set cli_pager "$CLI_PAGER"
}

main
