package it.pagopa.pn.ec.commons.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Setter
@ToString
@DynamoDbBean
public class DocumentVersion {

    Long version;

    @DynamoDbVersionAttribute
    public Long getVersion() { return version; }
}
