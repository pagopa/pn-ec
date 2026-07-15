package it.pagopa.pn.ec.commons.utils;


import lombok.CustomLog;
import reactor.util.retry.Retry;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@CustomLog
public class DynamoDbUtils {

    public static final Retry DYNAMO_OPTIMISTIC_LOCKING_RETRY = Retry.indefinitely()
            .filter(ConditionalCheckFailedException.class::isInstance)
            .doBeforeRetry(signal -> log.info("Optimistic locking retry #{} su ConditionalCheckFailedException",
                    signal.totalRetries() + 1));

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
