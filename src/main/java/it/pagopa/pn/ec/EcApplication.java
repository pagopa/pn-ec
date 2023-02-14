package it.pagopa.pn.ec;

import lombok.extern.slf4j.Slf4j;
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

//  <-- REPOSITORY MANAGER -->
// DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

//  <-- SMS -->
// SQS QUEUE
@PropertySource("classpath:sms/sms-sqs-queue.properties")

//  <-- EMAIL -->
// SQS QUEUE
@PropertySource("classpath:email/email-sqs-queue.properties")

//  <-- PEC -->
// SQS QUEUE
@PropertySource("classpath:pec/pec-sqs-queue.properties")

//  <-- NOTIFICATION TRACKER -->
// EVENTBRIDGE EVENT
@PropertySource("classpath:notificationtracker/notificationtracker-eventbridge-eventbuses.properties")

@Slf4j
public class EcApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcApplication.class, args);
    }
}
