package it.pagopa.pn.ec.testutils.localstack;

import it.pagopa.pn.ec.testutils.configuration.DynamoTestConfiguration;
import it.pagopa.pn.ec.testutils.configuration.SqsTestConfiguration;
import lombok.CustomLog;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@TestConfiguration
@Import({SqsTestConfiguration.class, DynamoTestConfiguration.class, LocalStackClientConfig.class})
@CustomLog
public class LocalStackTestConfig {

    static DockerImageName dockerImageName = DockerImageName.parse("localstack/localstack:1.0.4");
    static LocalStackContainer localStackContainer =
            new LocalStackContainer(dockerImageName).withServices(SQS, DYNAMODB, SNS, SES, SECRETSMANAGER, CLOUDWATCH, SSM, S3)
                    .withCopyFileToContainer(MountableFile.forClasspathResource("testcontainers/config"), "/config")
                    .withClasspathResourceMapping("testcontainers/init.sh", "/docker-entrypoint-initaws.d/make-storages.sh", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("testcontainers/credentials", "/root/.aws/credentials", BindMode.READ_ONLY)
                    .withEnv("RUNNING_IN_DOCKER", "true")
                    .withNetworkAliases("localstack")
                    .withNetwork(Network.builder().build())
                    .waitingFor(Wait.forLogMessage(".*Initialization terminated.*", 1));


    static {
        localStackContainer.start();
        //<-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
        //<-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

        System.setProperty("test.aws.event", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));

        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));

        System.setProperty("test.aws.ses.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SES)));

        System.setProperty("test.aws.secretsmanager.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SECRETSMANAGER)));

        System.setProperty("test.aws.cloudwatch.endpoint", String.valueOf(localStackContainer.getEndpointOverride(CLOUDWATCH)));

        System.setProperty("test.aws.ssm.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SSM)));

        System.setProperty("test.aws.s3.endpoint", String.valueOf(localStackContainer.getEndpointOverride(S3)));
    }

}
