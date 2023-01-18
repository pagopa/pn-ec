package it.pagopa.pn.ec.localstack;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static it.pagopa.pn.ec.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.startLocalStackContainer;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@TestConfiguration
public class SQSLocalStackTestConfig {

    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(SQS);

    static {
        startLocalStackContainer(localStack);
        System.setProperty("aws.sqs.test.endpoint", String.valueOf(localStack.getEndpointOverride(SQS)));
        try {
            localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", NOTIFICATION_TRACKER_QUEUE_NAME);
            localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", SMS_QUEUE_NAME);
            localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", SMS_ERROR_QUEUE_NAME);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
