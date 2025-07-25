package it.pagopa.pn.ec;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = {"it.pagopa.pn.ec", "it.pagopa.pn.library.pec","it.pagopa.pn.template"})
@ConfigurationPropertiesScan(basePackages = {"it.pagopa.pn.ec", "it.pagopa.pn.library.pec","it.pagopa.pn.template"})

// <-- COMMONS -->
// INTERNAL ENDPOINTS
@PropertySource("classpath:commons/internal-endpoint.properties")
// NOTIFICATION TRACKER QUEUE
@PropertySource("classpath:commons/notification-tracker-sqs-queue.properties")
// TRANSACTION PROCESS
@PropertySource("classpath:commons/transaction-process.properties")

//  <-- REPOSITORY MANAGER -->
// DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

//  <-- SMS -->
// SQS QUEUE
@PropertySource("classpath:sms/sms-sqs-queue.properties")
// SNS TOPIC
@PropertySource("classpath:sms/sms-sns-topic.properties")
// LAVORAZIONE
@PropertySource("classpath:sms/lavorazione-sms.properties")


//  <-- EMAIL -->
// SQS QUEUE
@PropertySource("classpath:email/email-sqs-queue.properties")
// LAVORAZIONE
@PropertySource("classpath:email/lavorazione-email.properties")

//  <-- PEC -->
// SQS QUEUE
@PropertySource("classpath:pec/pec-sqs-queue.properties")
// LAVORAZIONE
@PropertySource("classpath:pec/lavorazione-pec.properties")

//  <-- SCARICAMENTO ESITI PEC -->
@PropertySource("classpath:scaricamentoesitipec/scaricamento-esiti-pec.properties")
@PropertySource("classpath:scaricamentoesitipec/pn-pec-retry-strategy.properties")
@PropertySource("classpath:scaricamentoesitipec/cancellazione-ricevute-pec.properties")

//  <-- CARTACEO -->
// SQS QUEUE
@PropertySource("classpath:cartaceo/cartaceo-sqs-queue.properties")
// LAVORAZIONE
@PropertySource("classpath:cartaceo/lavorazione-cartaceo.properties")

//  <-- NOTIFICATION TRACKER -->
// EVENTBRIDGE EVENT
@PropertySource("classpath:notificationtracker/notificationtracker-eventbridge-eventbus.properties")

//LIBRARY PEC
@PropertySource("classpath:library/pec/providers.properties")

//S3
@PropertySource("classpath:s3/s3.properties")

//SQS
@PropertySource("classpath:sqs/sqs.properties")

//STATE MACHINE
@PropertySource("classpath:statemachine/statemachine.properties")

//NAMIRIAL
@PropertySource("classpath:namirial/namirial.properties")

//CLOUDWATCH
@PropertySource("classpath:cloudwatch/cloudwatch.properties")

//AVAILABILIYMANAGER
@PropertySource("classpath:availabilitymanager/availability-manager.properties")

//SERCQ
@PropertySource("classpath:sercq/sercq.properties")

//PDFRASTER
@PropertySource("classpath:pdfraster/pdf-raster.properties")

public class EcApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EcApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }
}
