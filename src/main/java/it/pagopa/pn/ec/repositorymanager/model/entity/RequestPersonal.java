package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDbBean
@Builder
public class RequestPersonal {

    @Getter(onMethod = @__({@DynamoDbPartitionKey}))
    String requestId;
    String xPagopaExtchCxId;
    OffsetDateTime clientRequestTimeStamp;
    OffsetDateTime requestTimestamp;
    DigitalRequestPersonal digitalRequestPersonal;
    PaperRequestPersonal paperRequestPersonal;
}
