package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class ClientConfiguration {

    @Getter(onMethod=@__({@DynamoDbPartitionKey}))
    @JsonProperty("xPagopaExtchCxId")
    String cxId;
    String sqsArn;
    String sqsName;
    String pecReplyTo;
    String mailReplyTo;
    SenderPhysicalAddress senderPhysicalAddress;
}
