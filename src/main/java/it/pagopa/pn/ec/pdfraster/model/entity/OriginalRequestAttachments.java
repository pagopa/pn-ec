package it.pagopa.pn.ec.pdfraster.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.math.BigDecimal;
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@DynamoDbBean
public class OriginalRequestAttachments {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("order")
    private BigDecimal order;

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("sha256")
    private String sha256;


}
