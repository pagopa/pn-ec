package it.pagopa.pn.ec.testutils.localstack;

import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.entity.Request;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.testutils.configuration.DynamoTestConfiguration;
import it.pagopa.pn.ec.testutils.configuration.SqsTestConfiguration;
import it.pagopa.pn.ec.testutils.exception.DynamoDbInitTableCreationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.ANAGRAFICA_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_TABLE_NAME;
import static java.util.Map.entry;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;
import static software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE;

@TestConfiguration
@Import({SqsTestConfiguration.class, DynamoTestConfiguration.class})
@Slf4j
public class LocalStackTestConfig {

    static DockerImageName dockerImageName = DockerImageName.parse("localstack/localstack:1.0.4");
    static LocalStackContainer localStackContainer =
            new LocalStackContainer(dockerImageName).withServices(SQS, DYNAMODB, SNS).withStartupTimeout(Duration.ofMinutes(2));

    static {
        localStackContainer.start();

//      <-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      <-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));

        System.setProperty("test.aws.event", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
        System.setProperty("event.Bus.Nome", "test-test");
        System.setProperty("statemachine.url", "statemachine-container-base-path-for-tests");
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
    }

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

    @Autowired
    private PecSqsQueueName pecSqsQueueName;

    private void initSqs() {
        log.info("<-- START initLocalStack.initSqs -->");

        List<String> notificationTrackerQueueNames = List.of(notificationTrackerSqsName.statoSmsName(),
                                                             notificationTrackerSqsName.statoSmsErratoName(),
                                                             notificationTrackerSqsName.statoEmailName(),
                                                             notificationTrackerSqsName.statoEmailErratoName(),
                                                             notificationTrackerSqsName.statoPecName(),
                                                             notificationTrackerSqsName.statoPecErratoName(),
                                                             notificationTrackerSqsName.statoCartaceoName(),
                                                             notificationTrackerSqsName.statoCartaceoErratoName());

        List<String> smsQueueNames = List.of(smsSqsQueueName.interactiveName(), smsSqsQueueName.batchName(), smsSqsQueueName.errorName());

        List<String> emailQueueNames = List.of(emailSqsQueueName.interactiveName(), emailSqsQueueName.batchName(), emailSqsQueueName.errorName());

        List<String> pecQueueNames = List.of(pecSqsQueueName.interactiveName(), pecSqsQueueName.batchName(), pecSqsQueueName.errorName());

        List<String> allQueueName = new ArrayList<>();
        allQueueName.addAll(notificationTrackerQueueNames);
        allQueueName.addAll(smsQueueNames);
        allQueueName.addAll(emailQueueNames);
        allQueueName.addAll(pecQueueNames);

        allQueueName.forEach(queueName -> {
            try {
                sqsClient.getQueueUrl(builder -> builder.queueName(queueName));
                log.info("Queue {} already created on local stack sqs", queueName);
            } catch (QueueDoesNotExistException queueDoesNotExistException) {
                log.info("Queue {} not found on first sqs init. Proceed to create", queueName);
                sqsClient.createQueue(builder -> builder.queueName(queueName));
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
                              entry(repositoryManagerDynamoTableName.richiesteName(), Request.class));

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
}
