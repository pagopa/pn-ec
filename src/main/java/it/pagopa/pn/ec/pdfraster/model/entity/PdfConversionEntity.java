package it.pagopa.pn.ec.pdfraster.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

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
}
