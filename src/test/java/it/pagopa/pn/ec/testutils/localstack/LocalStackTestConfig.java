package it.pagopa.pn.ec.testutils.localstack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@TestConfiguration
public class LocalStackTestConfig {

//  Oggetti dell'SDK che serviranno per la creazione delle tabelle Dynamo
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;

    static DockerImageName dockerImageName = DockerImageName.parse("localstack/localstack:latest");
    static LocalStackContainer localStackContainer = new LocalStackContainer(dockerImageName).withServices(SQS, DYNAMODB, SNS);

    static {
        localStackContainer.start();

//      <-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      <-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.sqs.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));

        try {

//          Create SQS queue
            for (String queueName : ALL_QUEUE_NAME_LIST) {
                localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queueName);
            }

            // TODO: Create DynamoDb schemas
            // TODO: Create SNS topic
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void createTable() {

        //Esempio di creazione tabelle dynamo all'avvio del container

        /*
            DynamoDbTable<'Classe che rappresenta lo schema Dynamo'> table = enhancedClient.table('Nome tabella',
                                                                                               TableSchema.fromBean('Classe che rappresenta lo schema Dynamo'.class));

            table.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
                                                                                                .writeCapacityUnits(5L)
                                                                                                .build()));

            // La creazione delle tabelle su Dynamo è asincrona. Bisogna aspettare tramite il DynamoDbWaiter

            ResponseOrException<DescribeTableResponse> response = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(
                    'Nome tabella').build()).matched();
            DescribeTableResponse tableDescription = response.response()
                                                             .orElseThrow(() -> new RuntimeException());
            // The actual error can be inspected in response.exception()
         */
    }
}
