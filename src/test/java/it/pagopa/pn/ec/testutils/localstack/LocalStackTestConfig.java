package it.pagopa.pn.ec.testutils.localstack;


import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.pdfraster.model.entity.PdfConversionEntity;
import it.pagopa.pn.ec.pdfraster.model.entity.RequestConversionEntity;
import it.pagopa.pn.ec.commons.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.ScaricamentoEsitiPecProperties;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.testutils.configuration.DynamoTestConfiguration;
import it.pagopa.pn.ec.testutils.configuration.SqsTestConfiguration;
import it.pagopa.pn.ec.testutils.exception.DynamoDbInitTableCreationException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;
import static software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE;

@TestConfiguration
@Import({SqsTestConfiguration.class, DynamoTestConfiguration.class, LocalStackClientConfig.class})
@CustomLog
public class LocalStackTestConfig {

    static DockerImageName dockerImageName = DockerImageName.parse("localstack/localstack:1.0.4");
    static LocalStackContainer localStackContainer =
            new LocalStackContainer(dockerImageName).withServices(SQS, DYNAMODB, SNS, SES, SECRETSMANAGER,CLOUDWATCH,SSM, S3)
                    .withStartupTimeout(Duration.ofMinutes(2)).withEnv("AWS_DEFAULT_REGION", "eu-central-1");

    static {
        localStackContainer.start();

//      <-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
//      <-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
        System.setProperty("test.aws.event", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));

        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));

        System.setProperty("test.aws.ses.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SES)));

        System.setProperty("test.aws.secretsmanager.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SECRETSMANAGER)));

        System.setProperty("test.aws.cloudwatch.endpoint", String.valueOf(localStackContainer.getEndpointOverride(CLOUDWATCH)));

        System.setProperty("test.aws.ssm.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SSM)));

        System.setProperty("test.aws.s3.endpoint", String.valueOf(localStackContainer.getEndpointOverride(S3)));

        try {
            localStackContainer.execInContainer("awslocal",
                    "secretsmanager",
                    "create-secret",
                    "--name",
                    "pn/identity/pec",
                    "--secret-string",
                    "{\"aruba.pec.username\":\"aruba_username@dgsspa.com\",\"aruba.pec.password\":\"aruba_password\",\"aruba.pec.sender\":\"aruba_sender@dgsspa.com\", \"namirial.pec.sender\":\"namirial_sender@dgsspa.com\"}");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            localStackContainer.execInContainer("awslocal",
                    "ssm",
                    "put-parameter",
                    "--name",
                    "pn-EC-esitiCartaceo",
                    "--type",
                    "String",
                    "--value",
                    "{\n" +
                            "    \"cartaceo\": {\n" +
                            "        \"RECRN004A\": {\n" +
                            "            \"deliveryFailureCause\": [\n" +
                            "                \"M05\",\n" +
                            "                \"M06\",\n" +
                            "                \"M07\"\n" +
                            "            ]\n" +
                            "        },\n" +
                            "        \"RECRN004B\": {\n" +
                            "            \"deliveryFailureCause\": [\n" +
                            "                \"M08\",\n" +
                            "                \"M09\",\n" +
                            "                \"F01\",\n" +
                            "                \"F02\",\n" +
                            "                \"TEST\"\n" +
                            "            ]\n" +
                            "        },\n" +
                            "        \"RECRN006\": {\n" +
                            "            \"deliveryFailureCause\": [\n" +
                            "                \"M03\",\n" +
                            "                \"M04\"\n" +
                            "            ]\n" +
                            "        }\n" +
                            "    }\n" +
                            "}");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            localStackContainer.execInContainer("awslocal",
                    "ssm",
                    "put-parameter",
                    "--name",
                    "Pn-EC-Pec-MetricsSchema",
                    "--type",
                    "String",
                    "--value",
                    "{\n" +
                            "    \"PayloadSizeRange\": {\n" +
                            "        \"0k-10k\": [ 0, 10 ],\n" +
                            "        \"10k-100k\": [ 10, 100 ],\n" +
                            "        \"100k+\": [ 100 ]\n" +
                            "    },\n" +
                            "    \"MessageCountRange\": {\n" +
                            "        \"0-10\": [ 0, 10 ],\n" +
                            "        \"10-100\": [ 10, 100 ],\n" +
                            "        \"100+\": [ 100 ]\n" +
                            "    }\n" +
                            "}");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void createTable(final String tableName, final Class<?> entityClass) {
        DynamoDbTable<?> dynamoDbTable = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(entityClass));
        dynamoDbTable.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L).writeCapacityUnits(5L).build()));

        // La creazione delle tabelle su Dynamo è asincrona. Bisogna aspettare tramite il DynamoDbWaiter
        ResponseOrException<DescribeTableResponse> responseOrException =
                dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(tableName).build()).matched();
        responseOrException.response().orElseThrow(() -> new DynamoDbInitTableCreationException(tableName));
    }

    @PostConstruct
    public void initLocalStack() {
        initSqs();
        initDynamo();
        initS3();
    }

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private AwsConfigurationProperties awsConfigurationProperties;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

    @Autowired
    private PecSqsQueueName pecSqsQueueName;

    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    private ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties;

    private void initSqs() {
        log.info("<-- START initLocalStack.initSqs -->");

        List<String> notificationTrackerQueueNames = List.of(notificationTrackerSqsName.statoSmsName(),
                notificationTrackerSqsName.statoSmsErratoName(),
                notificationTrackerSqsName.statoEmailName(),
                notificationTrackerSqsName.statoEmailErratoName(),
                notificationTrackerSqsName.statoPecName(),
                notificationTrackerSqsName.statoPecErratoName(),
                notificationTrackerSqsName.statoCartaceoName(),
                notificationTrackerSqsName.statoCartaceoErratoName(),
                notificationTrackerSqsName.statoSercqName(),
                notificationTrackerSqsName.statoSercqErratoName());

        List<String> smsQueueNames = List.of(smsSqsQueueName.interactiveName(), smsSqsQueueName.batchName(), smsSqsQueueName.errorName());

        List<String> emailQueueNames =
                List.of(emailSqsQueueName.interactiveName(), emailSqsQueueName.batchName(), emailSqsQueueName.errorName());
        List<String> cartceoQueueNames = List.of(cartaceoSqsQueueName.batchName(), cartaceoSqsQueueName.errorName(), cartaceoSqsQueueName.dlqErrorName());
//        cartaceoSqsQueueName.interactiveName(),
        List<String> pecQueueNames = List.of(pecSqsQueueName.interactiveName(), pecSqsQueueName.batchName(), pecSqsQueueName.errorName(), scaricamentoEsitiPecProperties.sqsQueueName());

        List<String> allQueueName = new ArrayList<>();
        allQueueName.addAll(notificationTrackerQueueNames);
        allQueueName.addAll(smsQueueNames);
        allQueueName.addAll(emailQueueNames);
        allQueueName.addAll(pecQueueNames);
        allQueueName.addAll(cartceoQueueNames);


        allQueueName.forEach(queueName -> {
            try {
                sqsClient.getQueueUrl(builder -> builder.queueName(queueName));
                log.info("Queue {} already created on local stack sqs", queueName);
            } catch (QueueDoesNotExistException queueDoesNotExistException) {
                log.info("Queue {} not found on first sqs init. Proceed to create", queueName);
                Map<QueueAttributeName, String> fifoAttribute = new HashMap<>();
                fifoAttribute.put(QueueAttributeName.FIFO_QUEUE, Boolean.TRUE.toString());
                fifoAttribute.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, Boolean.TRUE.toString());
                sqsClient.createQueue(builder -> builder.queueName(queueName).attributes(fifoAttribute));
            }
        });
    }

    //  Oggetti dell'SDK che serviranno per la creazione delle tabelle Dynamo
    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;

    @Autowired
    private RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;

    private void initDynamo() {
        log.info("<-- START initLocalStack.initDynamo -->");

        Map<String, Class<?>> tableNameWithEntityClass =
                Map.ofEntries(entry(repositoryManagerDynamoTableName.anagraficaClientName(), ClientConfiguration.class),
                        entry(repositoryManagerDynamoTableName.richiestePersonalName(), RequestPersonal.class),
                        entry(repositoryManagerDynamoTableName.richiesteMetadataName(), RequestMetadata.class),
                        entry(repositoryManagerDynamoTableName.richiesteConversioneRequestName(), RequestConversionEntity.class),
                        entry(repositoryManagerDynamoTableName.richiesteConversionePdfName(), PdfConversionEntity.class)
                );

        tableNameWithEntityClass.forEach((tableName, entityClass) -> {
            try {
                DescribeTableResponse describeTableResponse = dynamoDbClient.describeTable(builder -> builder.tableName(tableName));
                if (describeTableResponse.table().tableStatus() == ACTIVE) {
                    log.info("Table {} already created on local stack dynamo db", tableName);
                }
            } catch (ResourceNotFoundException resourceNotFoundException) {
                log.info("Table {} not found on first dynamo init. Proceed to create", tableName);
                createTable(tableName, entityClass);
            }
        });
    }

    @Value("${pn.ec.storage.sqs.messages.staging.bucket}")
    private String storageSqsMessagesStagingBucket;
    @Autowired
    private S3Client s3TestClient;
    private void initS3() {
        List<String> bucketNames = List.of(storageSqsMessagesStagingBucket);

        ObjectLockConfiguration objectLockConfiguration = ObjectLockConfiguration.builder().objectLockEnabled(ObjectLockEnabled.ENABLED)
                .rule(ObjectLockRule.builder().defaultRetention(DefaultRetention.builder().days(1).mode(ObjectLockRetentionMode.GOVERNANCE).build()).build())
                .build();

        bucketNames.forEach(bucket -> {
            log.info("<-- START S3 init-->");
            try {
                s3TestClient.headBucket(builder -> builder.bucket(bucket));
                log.info("Bucket {} already created on local stack S3", bucket);
            } catch (NoSuchBucketException noSuchBucketException) {
                s3TestClient.createBucket(builder -> builder.bucket(bucket).objectLockEnabledForBucket(true));
                s3TestClient.putObjectLockConfiguration(builder -> builder.bucket(bucket).objectLockConfiguration(objectLockConfiguration));
                log.info("New bucket {} created on local stack S3", bucket);
            }
        });
    }

}
