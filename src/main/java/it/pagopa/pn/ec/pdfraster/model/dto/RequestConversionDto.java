package it.pagopa.pn.ec.pdfraster.model.dto;

import it.pagopa.pn.ec.pdfraster.model.entity.AttachmentToConvert;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode
public class RequestConversionDto {

    String requestId;
    String requestTimestamp;
    BigDecimal expiration;
    OriginalRequestDto originalRequest;
    List<AttachmentToConvert> attachments;
}
