package it.pagopa.pn.ec.testutils.localstack;

import it.pagopa.pn.ec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.ANAGRAFICA_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.REQUEST_TABLE_NAME;
import static it.pagopa.pn.ec.testutils.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@TestConfiguration
public class LocalStackTestConfig {

    //  Oggetti dell'SDK che serviranno per la creazione delle tabelle Dynamo
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;

    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(
            SQS,
            DYNAMODB,
            SNS);

    static {
        localStackContainer.start();

        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
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

        //Esempio di creazione tabelle dynamo all'avvio del container (Local Stack) - AnagraficaClient Pn-Ec


            DynamoDbTable<ClientConfiguration> table1 = enhancedClient.table(ANAGRAFICA_TABLE_NAME,
                                                                                               TableSchema.fromBean(ClientConfiguration.class));

            table1.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
                                                                                                .writeCapacityUnits(5L)
                                                                                                .build()));

            // La creazione delle tabelle su Dynamo è asincrona. Bisogna aspettare tramite il DynamoDbWaiter

            ResponseOrException<DescribeTableResponse> response1 = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(
                    ANAGRAFICA_TABLE_NAME).build()).matched();
            DescribeTableResponse tableDescription1 = response1.response()
                                                             .orElseThrow(() -> new RuntimeException());
            // The actual error can be inspected in response.exception()



        //Esempio di creazione tabelle dynamo all'avvio del container (Local Stack) - Request Pn-Ec

            DynamoDbTable<Request> table2 = enhancedClient.table(REQUEST_TABLE_NAME,
                                                                                    TableSchema.fromBean(Request.class));

            table2.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
                                                                                                .writeCapacityUnits(5L)
                                                                                                .build()));

            ResponseOrException<DescribeTableResponse> response2 = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(
                    REQUEST_TABLE_NAME).build()).matched();
            DescribeTableResponse tableDescription2 = response2.response()
                    .orElseThrow(() -> new RuntimeException());

    }
}
