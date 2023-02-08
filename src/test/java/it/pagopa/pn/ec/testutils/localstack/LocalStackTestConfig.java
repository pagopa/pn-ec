package it.pagopa.pn.ec.testutils.localstack;

import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.testutils.exception.DynamoDbInitTableCreationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.ANAGRAFICA_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_METADATA_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_PERSONAL_TABLE_NAME;
import static java.util.Map.entry;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;
import static software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE;

@TestConfiguration
@Slf4j
public class LocalStackTestConfig {

    //  Oggetti dell'SDK che serviranno per la creazione delle tabelle Dynamo
    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;

    static DockerImageName dockerImageName = DockerImageName.parse("localstack/localstack:1.0.4");
    static LocalStackContainer localStackContainer = new LocalStackContainer(dockerImageName).withServices(SQS, DYNAMODB, SNS)
                                                                                             .withStartupTimeout(Duration.ofMinutes(2));

    static {
        localStackContainer.start();

        System.setProperty("test.aws.region", localStackContainer.getRegion());

//      <-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      <-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));

        try {

//          Create SQS queue
            for (String queueName : ALL_QUEUE_NAME_LIST) {
                localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queueName);
            }

            // TODO: Create SNS topic
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final static Map<String, Class<?>> TABLE_NAME_WITH_ENTITY_CLASS =
            Map.ofEntries(entry(ANAGRAFICA_TABLE_NAME, ClientConfiguration.class), entry(REQUEST_TABLE_NAME, Request.class), entry(REQUEST_METADATA_TABLE_NAME, RequestMetadata.class), entry(REQUEST_PERSONAL_TABLE_NAME, RequestPersonal.class));

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
        TABLE_NAME_WITH_ENTITY_CLASS.forEach((tableName, entityClass) -> {
            log.info("<-- START initLocalStack -->");
            try {
                log.info("<-- START Dynamo db init-->");
                DescribeTableResponse describeTableResponse = dynamoDbClient.describeTable(builder -> builder.tableName(tableName));
                if (describeTableResponse.table().tableStatus() == ACTIVE) {
                    log.info("Table {} already created on local stack's dynamo db", tableName);
                }
            } catch (ResourceNotFoundException resourceNotFoundException) {
                log.info("Table {} not found on first dynamo init. Proceed to create", tableName);
                createTable(tableName, entityClass);
            }
        });
    }
}
