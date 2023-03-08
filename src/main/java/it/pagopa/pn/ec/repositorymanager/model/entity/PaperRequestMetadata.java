package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@DynamoDbBean
public class PaperRequestMetadata {

    private String iun;
    private String requestPaid;
    private String productType;
    private String printType;
    private Map<String, String> vas;
}
