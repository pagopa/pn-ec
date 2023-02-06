package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationTrackerQueueDto extends PresaInCaricoInfo {

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    ProcessId processId;
    String currentStatus;
    String nextStatus;

    public NotificationTrackerQueueDto(String requestIdx, String xPagopaExtchCxId, ProcessId processId, String currentStatus,
                                       String nextStatus) {
        super(requestIdx, xPagopaExtchCxId);
        this.processId = processId;
        this.currentStatus = currentStatus;
        this.nextStatus = nextStatus;
    }
}
