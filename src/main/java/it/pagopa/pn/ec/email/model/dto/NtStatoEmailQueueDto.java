package it.pagopa.pn.ec.email.model.dto;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class NtStatoEmailQueueDto  {

    String xPagopaExtchCxId;

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    ProcessId processId;
    String currentStatus;
    String nextStatus;
}
