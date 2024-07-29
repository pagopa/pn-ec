package it.pagopa.pn.ec.pdfraster.model.entity;


import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@DynamoDbBean
public class AttachmentToConvert {
    String originalFileKey;
    String newFileKey;
    Boolean converted;
}
