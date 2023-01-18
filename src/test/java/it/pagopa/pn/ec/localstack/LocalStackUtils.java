package it.pagopa.pn.ec.localstack;

import org.testcontainers.containers.localstack.LocalStackContainer;

public class LocalStackUtils {

    static final String DEFAULT_LOCAL_STACK_TAG = "localstack/localstack:latest";

    static void startLocalStackContainer(LocalStackContainer localStackContainer) {
        localStackContainer.start();
        System.setProperty("aws.config.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.config.secret.key", localStackContainer.getSecretKey());
        System.setProperty("aws.config.default.region", localStackContainer.getRegion());
    }
}
