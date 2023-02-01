package it.pagopa.pn.ec.pec.model.dto;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.constant.status.Status;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class NtStatoPecQueueDto {

    String xPagopaExtchCxId;

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    ProcessId processId;
    Status currentStatus;
    Status nextStatus;
}
