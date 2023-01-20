package it.pagopa.pn.ec.model.dto;

import it.pagopa.pn.ec.constant.ProcessId;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class PresaInCaricoInfoDto {

    String idClient;
    ProcessId processId;
    String currentStatus;
}
