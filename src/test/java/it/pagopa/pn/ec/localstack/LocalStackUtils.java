package it.pagopa.pn.ec.localstack;

public class LocalStackUtils {

    static final String DEFAULT_LOCAL_STACK_TAG = "localstack/localstack:latest";

    static String[] createQueueCliCommand(String queueName) {
        return new String[]{"awslocal", "sqs", "create-queue", "--queue-name", queueName};
    }
}
