package it.pagopa.pn.ec.commons.utils;

import org.junit.jupiter.api.*;
import software.amazon.awssdk.enhanced.dynamodb.Key;


class DynamoDbUtilsTest {

    @Test
    void testGetKeyString() {
        String partitionKey = "testPartitionKey";
        Key key = DynamoDbUtils.getKey(partitionKey);

        Assertions.assertEquals(partitionKey, key.partitionKeyValue().s());
    }

    @Test
    void testGetKeyNumber() {
        Number partitionKey = 1;
        Key key = DynamoDbUtils.getKey(partitionKey);

        Assertions.assertEquals(partitionKey.toString(), key.partitionKeyValue().n());
    }
}

