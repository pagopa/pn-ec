package it.pagopa.pn.ec.testutils.localstack;

import it.pagopa.pn.ec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.ANAGRAFICA_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.REQUEST_TABLE_NAME;
import static java.util.Map.entry;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;
import static software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE;

@TestConfiguration
@Slf4j
public class LocalStackTestConfig {

    //  Oggetti dell'SDK che serviranno per la creazione delle tabelle Dynamo
    @Autowired
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    @Autowired
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    private DynamoDbAsyncWaiter dynamoDbAsyncWaiter;

    static DockerImageName dockerImageName = DockerImageName.parse("localstack/localstack:1.0.4");
    static LocalStackContainer localStackContainer = new LocalStackContainer(dockerImageName).withServices(SQS, DYNAMODB, SNS);

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

    private final static Map<String, Class<?>> tableNameWithEntityClass = Map.ofEntries(entry(ANAGRAFICA_TABLE_NAME,
                                                                                              ClientConfiguration.class),
                                                                                        entry(REQUEST_TABLE_NAME, Request.class));

    private Mono<Void> createTable(final String tableName, final Class<?> entityClass) {
        return Mono.fromFuture(dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(entityClass))
                                                          .createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
                                                                                                                      .writeCapacityUnits(5L)
                                                                                                                      .build())))
                   .flatMap(createTableResponse -> Mono.fromFuture(dynamoDbAsyncWaiter.waitUntilTableExists(builder -> builder.tableName(
                           tableName))))
                   .then();
    }

    @PostConstruct
    public void initDynamo() {

        Flux.fromStream(tableNameWithEntityClass.entrySet().stream())
            .doOnNext(stringClassEntry -> log.info("Creating '{}' table", stringClassEntry.getKey()))
            .flatMap(stringClassEntry -> Mono.fromFuture(dynamoDbAsyncClient.describeTable(builder -> builder.tableName(stringClassEntry.getKey())))
                                             .handle((describeTableResponse, synchronousSink) -> {
                                                 if (describeTableResponse.table().tableStatus() == ACTIVE) {
                                                     log.info("Table {} already exist, skip creation",
                                                              stringClassEntry.getKey());
                                                     synchronousSink.complete();
                                                 }
                                             })
                                             .flatMap(describeTableResponse -> createTable(stringClassEntry.getKey(),
                                                                                           stringClassEntry.getValue()))
                                             .onErrorResume(ResourceNotFoundException.class, throwable -> {
                                                 log.info("Table {} not found on first dynamo init. Proceed to create",
                                                          stringClassEntry.getKey());
                                                 return createTable(stringClassEntry.getKey(), stringClassEntry.getValue());
                                             }))
            .subscribe();
    }
}
