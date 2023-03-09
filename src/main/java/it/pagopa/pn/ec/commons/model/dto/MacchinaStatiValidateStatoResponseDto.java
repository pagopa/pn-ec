package it.pagopa.pn.ec.commons.model.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public class MacchinaStatiValidateStatoResponseDto {

    String message;
    boolean allowed;
    String notificationMessage;
}
