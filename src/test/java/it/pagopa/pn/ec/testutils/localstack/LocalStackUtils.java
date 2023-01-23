package it.pagopa.pn.ec.testutils.localstack;

public class LocalStackUtils {

    private LocalStackUtils() {
        throw new IllegalStateException("LocalStackUtils is utility class");
    }

    static final String DEFAULT_LOCAL_STACK_TAG = "localstack/localstack:latest";
}
