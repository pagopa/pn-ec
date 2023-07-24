package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.ec.commons.model.entity.DocumentVersion;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
@EqualsAndHashCode(callSuper = true)
public class ClientConfigurationInternal extends DocumentVersion {

    @Getter(onMethod=@__({@DynamoDbPartitionKey}))
    @JsonProperty("xPagopaExtchCxId")
    String cxId;
    String sqsArn;
    String sqsName;
    String pecReplyTo;
    String mailReplyTo;
    @ToString.Exclude
    String apiKey;
    @ToString.Exclude
    SenderPhysicalAddress senderPhysicalAddress;
}

