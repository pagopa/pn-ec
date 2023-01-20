package it.pagopa.pn.ec.localstack;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static it.pagopa.pn.ec.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.createQueueCliCommand;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@TestConfiguration
public class LocalStackTestConfig {

    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(
            SQS,
            DYNAMODB,
            SNS);

    static {
        localStackContainer.start();

//      Override aws config
        System.setProperty("AWS_ACCESS_KEY", localStackContainer.getAccessKey());
        System.setProperty("AWS_SECRET_KEY", localStackContainer.getSecretKey());
        System.setProperty("AWS_DEFAULT_REGION", localStackContainer.getRegion());

//      SQS Override Endpoint
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      DynamoDb Override Endpoint
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));

//      SNS Override Endpoint
        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));
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
}
