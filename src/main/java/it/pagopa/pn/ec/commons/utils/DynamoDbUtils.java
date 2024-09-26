package it.pagopa.pn.ec.commons.utils;

import reactor.util.retry.Retry;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

public class DynamoDbUtils {

    public static final Retry DYNAMO_OPTIMISTIC_LOCKING_RETRY = Retry.indefinitely().filter(ConditionalCheckFailedException.class::isInstance);

    private DynamoDbUtils() {
        throw new IllegalStateException("DynamoDbUtils is a utility class");
    }


    public static Key getKey(String partitionKey) {
        return Key.builder()
                  .partitionValue(partitionKey)
                  .build();
    }

    public static Key getKey(Number partitionKey) {
        return Key.builder()
                  .partitionValue(partitionKey)
                  .build();
    }
}
