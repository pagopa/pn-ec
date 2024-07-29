package it.pagopa.pn.ec.pdfraster.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OriginalRequestAttachmentsDto {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("order")
    private BigDecimal order;

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("sha256")
    private String sha256;

}
