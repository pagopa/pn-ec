package it.pagopa.pn.ec.pdfraster.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode
public class AttachmentToConvertDto {
    String originalFileKey;
    String newFileKey;
    Boolean converted;
}
