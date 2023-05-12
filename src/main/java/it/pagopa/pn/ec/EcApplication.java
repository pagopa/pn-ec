package it.pagopa.pn.ec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ConfigurationPropertiesScan

// <-- COMMONS -->
// AWS CONFIGURATION
@PropertySource("classpath:commons/aws-configuration.properties")
// INTERNAL ENDPOINTS
@PropertySource("classpath:commons/internal-endpoint.properties")
// NOTIFICATION TRACKER QUEUE
@PropertySource("classpath:commons/notification-tracker-sqs-queue.properties")
// TRANSACTION PROCESS
@PropertySource("classpath:commons/transaction-process.properties")
// SCHEDULED CRON VALUES
@PropertySource("classpath:commons/scheduled-cron.properties")

//  <-- REPOSITORY MANAGER -->
// DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

//  <-- SMS -->
// SQS QUEUE
@PropertySource("classpath:sms/sms-sqs-queue.properties")
// SNS TOPIC
@PropertySource("classpath:sms/sms-sns-topic.properties")

//  <-- EMAIL -->
// SQS QUEUE
@PropertySource("classpath:email/email-sqs-queue.properties")
// DEFAULT
@PropertySource("classpath:email/email.properties")

//  <-- PEC -->
// SQS QUEUE
@PropertySource("classpath:pec/pec-sqs-queue.properties")

//  <-- SCARICAMENTO ESITI PEC -->
@PropertySource("classpath:scaricamentoesitipec/scaricamento-esiti-pec.properties")

//  <-- CARTACEO -->
// SQS QUEUE
@PropertySource("classpath:cartaceo/cartaceo-sqs-queue.properties")

//  <-- NOTIFICATION TRACKER -->
// EVENTBRIDGE EVENT
@PropertySource("classpath:notificationtracker/notificationtracker-eventbridge-eventbus.properties")
public class EcApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcApplication.class, args);
    }
}
