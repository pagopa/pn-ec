package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.constant.status.Status;
import it.pagopa.pn.ec.commons.model.pojo.RequestBaseInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationTrackerQueueDto extends RequestBaseInfo {

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    ProcessId processId;
    Status currentStatus;
    Status nextStatus;

    public NotificationTrackerQueueDto(RequestBaseInfo requestBaseInfo, Status currentStatus, Status nextStatus) {
        super(requestBaseInfo.getIdRequest(), requestBaseInfo.getIdClient());
        this.currentStatus = currentStatus;
        this.nextStatus = nextStatus;
    }
}
