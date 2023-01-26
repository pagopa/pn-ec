package it.pagopa.pn.ec.repositorymanager.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@ToString
@DynamoDbBean
public class ClientConfiguration {

    @Getter(AccessLevel.NONE)
    String cxId;
    String sqsArn;
    String sqsName;
    String pecReplyTo;
    String mailReplyTo;
    SenderPhysicalAddress senderPhysicalAddress;

    @DynamoDbPartitionKey
    public String getCxId() {
        return cxId;
    }
}
