#!/bin/bash

set -e

# Configura gli endpoint e la regione AWS
VERBOSE=true
AWS_REGION="eu-south-1"
LOCALSTACK_ENDPOINT="http://localhost:4566"

SQS_QUEUES=(
  "pn-ec-tracker-sms-stato-queue.fifo"
  "pn-ec-tracker-sms-errori-queue.fifo"
  "pn-ec-sms-batch-queue.fifo"
  "pn-ec-sms-interactive-queue.fifo"
  "pn-ec-sms-errori-queue.fifo"
  "pn-ec-sms-errori-queue-DLQ.fifo"

  "pn-ec-tracker-email-stato-queue.fifo"
  "pn-ec-tracker-email-errori-queue.fifo"
  "pn-ec-email-interactive-queue.fifo"
  "pn-ec-email-batch-queue.fifo"
  "pn-ec-email-errori-queue.fifo"
  "pn-ec-email-errori-queue-DLQ.fifo"

  "pn-ec-tracker-pec-stato-queue.fifo"
  "pn-ec-tracker-pec-errori-queue.fifo"
  "pn-ec-pec-batch-queue.fifo"
  "pn-ec-pec-interactive-queue.fifo"
  "pn-ec-pec-errori-queue.fifo"
  "pn-ec-pec-errori-queue-DLQ.fifo"
  "pn-ec-pec-scaricamento-esiti-queue.fifo"
  "pn-ec-pec-cancellazione-ricevute-queue.fifo"

  "pn-ec-tracker-cartaceo-stato-queue.fifo"
  "pn-ec-tracker-cartaceo-errori-queue.fifo"
  "pn-ec-cartaceo-batch-queue.fifo"
  "pn-ec-cartaceo-errori-queue.fifo"
  "pn-ec-cartaceo-errori-queue-DLQ.fifo"

  "pn-ec-tracker-sercq-send-stato-queue.fifo"
  "pn-ec-tracker-sercq-send-errori-queue.fifo"

  "pn-ec-availabilitymanager-queue.fifo"
)

S3_BUCKETS=(
  "pn-sqs-messages-staging"
)

SES_EMAILS=(
  "test@pagopa.com"
)

DYNAMODB_TABLES=(
  "pn-EcAnagrafica:cxId"
  "pn-EcRichieste:requestId"
  "pn-EcRichiesteMetadati:requestId"
  "pn-EcRichiesteConversione:requestId"
  "pn-EcConversionePDF:fileKey"
)


## LOGGING FUNCTIONS ##
log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

silent() {
  if [ "$VERBOSE" = false ]; then
    "$@" > /dev/null 2>&1
  else
    "$@"
  fi
}

## HELPER FUNCTIONS ##
wait_for_pids() {
  local -n pid_array=$1
  local error_message=$2
  local exit_code=0

  for pid in "${pid_array[@]}"; do
    wait "$pid" || { log "$error_message"; exit_code=1; }
  done

  return "$exit_code"
}

# Creazione delle tabelle DynamoDB
create_dynamodb_table() {
  local table_name=$1
  local pk=$2

  log "Creating DynamoDB table: $table_name"
  if ! silent aws dynamodb describe-table --table-name "$table_name" --region "$AWS_REGION" --endpoint-url "$LOCALSTACK_ENDPOINT" ; then
    if ! aws dynamodb create-table \
      --region "$AWS_REGION" \
      --endpoint-url "$LOCALSTACK_ENDPOINT" \
      --table-name "$table_name" \
      --attribute-definitions AttributeName="$pk",AttributeType=S \
      --key-schema AttributeName="$pk",KeyType=HASH \
      --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 ; then
      log "Failed to create table: $table_name"
      return 1
    else
      log "Table created: $table_name"
    fi
  else
    log "Table already exists: $table_name"
  fi
}

# Creazione delle code SQS
create_sqs_queue() {
  local queue_name=$1
  log "Creating SQS queue: $queue_name"

  silent aws sqs create-queue \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --queue-name "$queue_name" \
    --attributes FifoQueue=true,ContentBasedDeduplication=true && \
    log "Queue created: $queue_name" || \
  { log "Failed to create queue: $queue_name"; return 1; }
}

# Creazione dei bucket S3
create_s3_bucket() {
  local bucket_name=$1
  echo "Crating S3 bucket: $bucket_name"

  silent aws s3api head-bucket \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --bucket "$bucket_name" && \
  log "Bucket already exists: $bucket_name" && return 0

  aws s3api create-bucket \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --bucket "$bucket_name" \
    --create-bucket-configuration LocationConstraint="$AWS_REGION" \
    --object-lock-enabled-for-bucket && \
  aws s3api put-object-lock-configuration \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --bucket "$bucket_name" \
    --object-lock-configuration "ObjectLockEnabled=Enabled,Rule={DefaultRetention={Days=1,Mode=GOVERNANCE}}" && \
    log "Bucket created: $bucket_name" || \
  { log "Failed to create bucket: $bucket_name"; return 1; }
}

# Creazione dei parametri SSM
create_ssm_parameter() {
  local parameter_name=$1
  local parameter_value=$2
  echo "Creating parameter: $parameter_name"
  echo "Parameter value: $parameter_value"

  silent aws ssm get-parameter \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$parameter_name" && \
    log "Parameter already exists: $parameter_name" && \
    return 0

  aws ssm put-parameter \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$parameter_name" \
    --type String \
    --value "$parameter_value" && \
    log "Parameter created: $parameter_name" || \
  { log "Failed to create parameter: $parameter_name"; return 1; }
}

# Creazione dei segreti Secrets Manager
create_secret() {
  local secret_name=$1
  local secret_value=$2
  echo "Creating secret: $secret_name"
  echo "Secret value: $secret_value"

  silent aws secretsmanager get-secret-value \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --secret-id "$secret_name" && \
    log "Secret already exists: $secret_name" && \
    return 0

  aws secretsmanager create-secret \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$secret_name" \
    --secret-string "$secret_value" && \

  aws secretsmanager put-secret-value \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --secret-id "$secret_name" \
    --secret-string "$secret_value" && \
    log "Secret created: $secret_name" || \
  { log "Failed to create secret: $secret_name"; return 1; }
}

authorize_ses_email() {
  local email_address=$1
  aws ses verify-email-identity \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --email-address "$email_address" && \
    log "Email address verified: $email_address" || \
  { log "Failed to verify email address: $email_address"; return 1; }
}

create_queues(){
  local pids=()
  for queue in "${SQS_QUEUES[@]}"; do
    silent create_sqs_queue "$queue" &
    pids+=("$!")
  done
  wait_for_pids pids "Failed to create SQS queues"
  return $?
}

create_buckets(){
  local pids=()
  for bucket in "${S3_BUCKETS[@]}"; do
    silent create_s3_bucket "$bucket" &
    pids+=("$!")
  done
  wait_for_pids pids "Failed to create S3 buckets"
  return $?
}

authorize_emails(){
  local pids=()
  for email in "${SES_EMAILS[@]}"; do
    silent authorize_ses_email "$email" &
    pids+=("$!")
  done
  wait_for_pids pids "Failed to authorize SES emails"
  return $?
}

initialize_dynamo() {
  log "Initializing DynamoDB tables"
  local return_code=0

  for entry in "${DYNAMODB_TABLES[@]}"; do
    IFS=: read -r table_name pk <<< "$entry"
    silent create_dynamodb_table "$table_name" "$pk" && \
    log "Table initialized: $table_name" || \
    { log "Failed to initialize table: $table_name"; return_code=1; }
  done
  return $return_code
}

# Main
main(){
  start_time=$(date +%s)
  pids=()
  create_queues &
  pids+=("$!")
  create_buckets &
  pids+=("$!")
  authorize_emails &
  pids+=("$!")
  initialize_dynamo &
  pids+=("$!")
  create_ssm_parameter "pn-EC-esitiCartaceo" '{
    "cartaceo": {
      "RECRN004A": {"deliveryFailureCause": ["M05", "M06", "M07"]},
      "RECRN004B": {"deliveryFailureCause": ["M08", "M09", "F01", "F02", "TEST"]},
      "RECRN006": {"deliveryFailureCause": ["M03", "M04"]}
    }
  }' &
  pids+=("$!")
  create_secret "Pn-EC-PEC" '{
    "aruba.pec.username": "aruba_username@dgsspa.com",
    "aruba.pec.password": "aruba_password",
    "aruba.pec.sender": "aruba_sender@dgsspa.com",
    "namirial.pec.sender": "namirial_sender@dgsspa.com"
  }' &
  pids+=("$!")

  wait_for_pids pids "Failed to initialize components" && \
  log "Initialization completed successfully" || \
  { log "Failed to initialize components"; exit 1; }
  end_time=$(date +%s)

  log "Execution time: $((end_time - start_time)) seconds"
}


# Esecuzione del main
main
echo "Initialization terminated"
