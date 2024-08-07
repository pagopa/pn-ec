package it.pagopa.pn.ec.pdfraster.model.entity;

import it.pagopa.pn.ec.commons.model.entity.DocumentVersion;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@DynamoDbBean
public class RequestConversionEntity extends DocumentVersion {

    @Getter(onMethod=@__({@DynamoDbPartitionKey}))
    String requestId;
    String requestTimestamp;
    BigDecimal expiration;
    OriginalRequest originalRequest;
    List<AttachmentToConvert> attachments;
}
