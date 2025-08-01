package it.pagopa.pn.ec.pdfraster.model.entity;

import it.pagopa.pn.ec.commons.model.entity.DocumentVersion;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@DynamoDbBean
public class PdfConversionEntity {

    @Getter(onMethod=@__({@DynamoDbPartitionKey}))
    String fileKey;
    String requestId;
    BigDecimal expiration;
    Long version;
    String transformationError;
}
