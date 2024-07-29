package it.pagopa.pn.ec.pdfraster.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode
public class PdfConversionDto {

    String fileKey;
    String requestId;
    BigDecimal expiration;
}
