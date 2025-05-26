#!/bin/bash
COMMIT_ID=${1:-develop}
set -euo pipefail

# Configurazione
VERBOSE=false
REGION=us-east-1
AWS_PROFILE="default"
ACCESS_KEY=TEST
SECRET_KEY=TEST

# Logging
log() { echo "[pn-ec-init][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

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
  local start=$(date +%s)

  execute_init
  load_dynamodb

  local duration=$(( $(date +%s) - start ))
  log "init-for-localdev.sh executed in ${duration}s"
}

main
