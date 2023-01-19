package it.pagopa.pn.ec.localstack;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static it.pagopa.pn.ec.constant.QueueNameConstant.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@TestConfiguration
public class LocalStackTestConfig {

    static LocalStackContainer localStackContainer =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest")).withServices(
            SQS,
            DYNAMODB);

    static {
        localStackContainer.start();
        System.setProperty("aws.config.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.config.secret.key", localStackContainer.getSecretKey());
        System.setProperty("aws.config.default.region", localStackContainer.getRegion());

//      SQS Override Endpoint
        System.setProperty("aws.sqs.test.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      DynamoDb Override Endpoint
        System.setProperty("aws.dynamodb.test.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        try {

//          Create SQS queue
            localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", NOTIFICATION_TRACKER_QUEUE_NAME);
            localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", SMS_QUEUE_NAME);
            localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", SMS_ERROR_QUEUE_NAME);

            // TODO: Create DynamoDb schemas
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
