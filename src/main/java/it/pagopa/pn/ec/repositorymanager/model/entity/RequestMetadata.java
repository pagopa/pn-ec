package it.pagopa.pn.ec.repositorymanager.model.entity;

import it.pagopa.pn.ec.commons.model.entity.DocumentVersion;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.OffsetDateTime;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
@EqualsAndHashCode(callSuper = true)
public class RequestMetadata extends DocumentVersion {

    @Getter(onMethod = @__({@DynamoDbPartitionKey}))
    String requestId;
    String messageId;
    String xPagopaExtchCxId;
    String statusRequest;
    String requestHash;
    OffsetDateTime clientRequestTimeStamp;
    OffsetDateTime requestTimestamp;
    DigitalRequestMetadata digitalRequestMetadata;
    PaperRequestMetadata paperRequestMetadata;
    List<Events> eventsList;
    Retry retry;
}
