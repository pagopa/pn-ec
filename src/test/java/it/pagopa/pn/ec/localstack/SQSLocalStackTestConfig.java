package it.pagopa.pn.ec.localstack;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@TestConfiguration
public class SQSLocalStackTestConfig {

    static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest")).withServices(SQS);

    static {
        localStack.start();
        System.setProperty("aws.config.access.key", localStack.getAccessKey());
        System.setProperty("aws.config.secret.key", localStack.getSecretKey());
        System.setProperty("aws.config.default.region", localStack.getRegion());
        System.setProperty("aws.sqs.test.endpoint", String.valueOf(localStack.getEndpointOverride(SQS)));
        try {
            localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "pn-ec-notification-tracker-queue-temp");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
