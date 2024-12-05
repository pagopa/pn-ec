#!/bin/bash

set -e

# Configura gli endpoint e la regione AWS
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

  "pn-ec-tracker-cartaceo-stato-queue.fifo"
  "pn-ec-tracker-cartaceo-errori-queue.fifo"
  "pn-ec-cartaceo-batch-queue.fifo"
  "pn-ec-cartaceo-errori-queue.fifo"
  "pn-ec-cartaceo-errori-queue-DLQ.fifo"

  "pn-ec-tracker-sercq-send-stato-queue.fifo"
  "pn-ec-tracker-sercq-send-errori-queue.fifo"
)

S3_BUCKETS=(
  "pn-ec-storage-sqs-messages-staging"
)

SES_EMAILS=(
  "test@pagopa.com"
)

# Creazione delle tabelle DynamoDB
create_dynamodb_table() {
  local table_name=$1
  local pk=$2

  echo "Creating DynamoDB table: $table_name"
  aws dynamodb create-table \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --table-name "$table_name" \
    --attribute-definitions AttributeName="$pk",AttributeType=S \
    --key-schema AttributeName="$pk",KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
}

# Creazione delle code SQS
create_sqs_queue() {
  local queue_name=$1
  echo "Creating SQS queue: $queue_name"
  aws sqs create-queue \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --queue-name "$queue_name" \
    --attributes FifoQueue=true,ContentBasedDeduplication=true
}

# Creazione dei bucket S3
create_s3_bucket() {
  local bucket_name=$1
  echo "Crating S3 bucket: $bucket_name"
  aws s3api create-bucket \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --bucket "$bucket_name" \
    --create-bucket-configuration LocationConstraint="$AWS_REGION" \
    --object-lock-enabled-for-bucket || true

  aws s3api put-object-lock-configuration \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --bucket "$bucket_name" \
    --object-lock-configuration "ObjectLockEnabled=Enabled,Rule={DefaultRetention={Days=1,Mode=GOVERNANCE}}"
}

# Creazione dei parametri SSM
create_ssm_parameter() {
  local parameter_name=$1
  local parameter_value=$2
  echo "Creating parameter: $parameter_name"
  aws ssm put-parameter \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$parameter_name" \
    --type String \
    --value "$parameter_value" || true
}

# Creazione dei segreti Secrets Manager
create_secret() {
  local secret_name=$1
  local secret_value=$2
  echo "Creating secret: $secret_name"
  aws secretsmanager create-secret \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$secret_name" \
    --secret-string "$secret_value"
}

authorize_ses_email() {
  local email_address=$1
  aws ses verify-email-identity \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --email-address "$email_address"
}

for queue in "${SQS_QUEUES[@]}"; do
  create_sqs_queue "$queue" &
done
wait

for bucket in "${S3_BUCKETS[@]}"; do
  create_s3_bucket "$bucket" &
done
wait

for email in "${SES_EMAILS[@]}"; do
  authorize_ses_email "$email" &
done
wait

create_dynamodb_table "pn-EcAnagrafica" "cxId"
create_dynamodb_table "pn-EcRichieste" "requestId"
create_dynamodb_table "pn-EcRichiesteMetadati" "requestId"
create_dynamodb_table "pn-EcRichiesteConversione" "requestId"
create_dynamodb_table "pn-EcConversionePDF" "fileKey"

create_ssm_parameter "pn-EC-esitiCartaceo" '{
  "cartaceo": {
    "RECRN004A": {"deliveryFailureCause": ["M05", "M06", "M07"]},
    "RECRN004B": {"deliveryFailureCause": ["M08", "M09", "F01", "F02", "TEST"]},
    "RECRN006": {"deliveryFailureCause": ["M03", "M04"]}
  }
}'

create_secret "pn/identity/pec" '{
  "aruba.pec.username": "aruba_username@dgsspa.com",
  "aruba.pec.password": "aruba_password",
  "aruba.pec.sender": "aruba_sender@dgsspa.com",
  "namirial.pec.sender": "namirial_sender@dgsspa.com"
}'

echo "Initialization terminated"
