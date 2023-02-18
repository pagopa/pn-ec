package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationTrackerQueueDto extends PresaInCaricoInfo {

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    OffsetDateTime eventTimestamp;
    String processId;
    String currentStatus;
    String nextStatus;
    String eventDetails;
    GeneratedMessageDto generatedMessageDto;

    public NotificationTrackerQueueDto(String requestIdx, String xPagopaExtchCxId, OffsetDateTime eventTimestamp, String processId,
                                       String currentStatus, String nextStatus, GeneratedMessageDto generatedMessageDto) {
        super(requestIdx, xPagopaExtchCxId);
        this.eventTimestamp = eventTimestamp;
        this.processId = processId;
        this.currentStatus = currentStatus;
        this.nextStatus = nextStatus;
        this.generatedMessageDto = generatedMessageDto;
    }

    public NotificationTrackerQueueDto(String requestIdx, String xPagopaExtchCxId, OffsetDateTime eventTimestamp, String processId,
                                       String currentStatus, String nextStatus, String eventDetails,
                                       GeneratedMessageDto generatedMessageDto) {
        super(requestIdx, xPagopaExtchCxId);
        this.eventTimestamp = eventTimestamp;
        this.processId = processId;
        this.currentStatus = currentStatus;
        this.nextStatus = nextStatus;
        this.eventDetails = eventDetails;
        this.generatedMessageDto = generatedMessageDto;
    }
}
