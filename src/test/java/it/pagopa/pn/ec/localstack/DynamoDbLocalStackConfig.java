package it.pagopa.pn.ec.localstack;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static it.pagopa.pn.ec.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static it.pagopa.pn.ec.localstack.LocalStackUtils.startLocalStackContainer;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

@TestConfiguration
public class DynamoDbLocalStackConfig {

    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(DYNAMODB);

    static {
        startLocalStackContainer(localStack);
        System.setProperty("aws.dynamodb.test.endpoint", String.valueOf(localStack.getEndpointOverride(DYNAMODB)));
    }
}
