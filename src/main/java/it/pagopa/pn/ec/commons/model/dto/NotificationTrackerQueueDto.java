package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.constant.status.Status;
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
    Status currentStatus;
    Status nextStatus;

    public NotificationTrackerQueueDto(PresaInCaricoInfo requestBaseInfo, Status currentStatus, Status nextStatus) {
        super(requestBaseInfo.getRequestIdx(), requestBaseInfo.getXPagopaExtchCxId());
        this.currentStatus = currentStatus;
        this.nextStatus = nextStatus;
    }
}
