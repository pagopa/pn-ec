package it.pagopa.pn.ec.sms.model.dto;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.constant.status.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class NtStatoSmsQueueDto {

    String xPagopaExtchCxId;

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    ProcessId processId;
    Status currentStatus;
    Status nextStatus;
}
