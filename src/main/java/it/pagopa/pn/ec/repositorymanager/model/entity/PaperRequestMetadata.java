package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@DynamoDbBean
public class PaperRequestMetadata {

    String iun;
    String requestPaId;
    String productType;
    String printType;
    Map<String, String> vas;
    Boolean duplicateCheckPassthrough;
    Boolean isOpenReworkRequest;
}
