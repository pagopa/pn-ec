package it.pagopa.pn.ec.localstack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import it.pagopa.pn.ec.repositorymanager.model.ClientConfiguration;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.io.IOException;

import javax.annotation.PostConstruct;

import static it.pagopa.pn.ec.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.createQueueCliCommand;
import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.ANAGRAFICA_TABLE_NAME;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@TestConfiguration
public class LocalStackTestConfig {
	
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;

    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(
            SQS,
            DYNAMODB);

    static {
        localStackContainer.start();

//      Override aws config
        System.setProperty("aws.config.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.config.secret.key", localStackContainer.getSecretKey());
        System.setProperty("aws.config.default.region", localStackContainer.getRegion());

//      SQS Override Endpoint
        System.setProperty("aws.sqs.test.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      DynamoDb Override Endpoint
        System.setProperty("aws.dynamodb.test.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        try {

//          Create SQS queue
            localStackContainer.execInContainer(createQueueCliCommand(NOTIFICATION_TRACKER_QUEUE_NAME));
            localStackContainer.execInContainer(createQueueCliCommand(SMS_QUEUE_NAME));
            localStackContainer.execInContainer(createQueueCliCommand(SMS_ERROR_QUEUE_NAME));

            // TODO: Create DynamoDb schemas
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
	@PostConstruct
	public void createTable() {
		DynamoDbTable<ClientConfiguration> clientConfigurationTable = enhancedClient.table(ANAGRAFICA_TABLE_NAME,
				TableSchema.fromBean(ClientConfiguration.class));

		clientConfigurationTable.createTable(
				builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L).writeCapacityUnits(5L).build()));

		try (DynamoDbWaiter waiter = DynamoDbWaiter.create()) { // DynamoDbWaiter is Autocloseable
			ResponseOrException<DescribeTableResponse> response = waiter
					.waitUntilTableExists(builder -> builder.tableName(ANAGRAFICA_TABLE_NAME).build()).matched();
			DescribeTableResponse tableDescription = response.response()
					.orElseThrow(() -> new RuntimeException("Customer table was not created."));
			// The actual error can be inspected in response.exception()
		}
	}
    
}
