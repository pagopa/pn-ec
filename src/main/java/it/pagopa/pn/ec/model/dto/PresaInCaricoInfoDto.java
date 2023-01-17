package it.pagopa.pn.ec.model.dto;

import it.pagopa.pn.ec.constant.ProcessId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@Setter
public class PresaInCaricoInfoDto {

    String idClient;
    ProcessId processId;
    String currentStatus;
}
