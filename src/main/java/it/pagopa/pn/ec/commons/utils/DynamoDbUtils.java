package it.pagopa.pn.ec.commons.utils;

import software.amazon.awssdk.enhanced.dynamodb.Key;

public class DynamoDbUtils {

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
