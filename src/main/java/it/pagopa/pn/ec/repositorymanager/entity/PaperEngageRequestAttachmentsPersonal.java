package it.pagopa.pn.ec.repositorymanager.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.math.BigDecimal;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class PaperEngageRequestAttachmentsPersonal {

    private String uri;
    private BigDecimal order;
    private String documentType;
    private String sha256;
}
