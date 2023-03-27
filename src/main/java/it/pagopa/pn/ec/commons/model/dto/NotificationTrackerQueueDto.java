package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.RequestStatusChange;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import static java.time.OffsetDateTime.now;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class NotificationTrackerQueueDto extends RequestStatusChange {

    DigitalProgressStatusDto digitalProgressStatusDto;
    PaperProgressStatusDto paperProgressStatusDto;
    int retry;

    @SuppressWarnings("all")
    private static NotificationTrackerQueueDto createNotificationTrackerQueueDto(PresaInCaricoInfo presaInCaricoInfo, String currentStatus, String nextStatus) {
        return NotificationTrackerQueueDto.builder()
                                          .requestIdx(presaInCaricoInfo.getRequestIdx())
                                          .xPagopaExtchCxId(presaInCaricoInfo.getXPagopaExtchCxId())
                                          .currentStatus(currentStatus)
                                          .nextStatus(nextStatus)
                                          .build();
    }

    public static NotificationTrackerQueueDto createNotificationTrackerQueueDtoDigital(PresaInCaricoInfo presaInCaricoInfo,
                                                                                       String currentStatus, String nextStatus,
                                                                                       DigitalProgressStatusDto digitalProgressStatusDto) {
        var notificationTrackerQueueDto = createNotificationTrackerQueueDto(presaInCaricoInfo, currentStatus, nextStatus);
        digitalProgressStatusDto.setEventTimestamp(now());
        notificationTrackerQueueDto.setDigitalProgressStatusDto(digitalProgressStatusDto);
        return notificationTrackerQueueDto;
    }

    public static NotificationTrackerQueueDto createNotificationTrackerQueueDtoPaper(PresaInCaricoInfo presaInCaricoInfo,
                                                                                     String currentStatus, String nextStatus,
                                                                                     PaperProgressStatusDto paperProgressStatusDto) {
        var notificationTrackerQueueDto = createNotificationTrackerQueueDto(presaInCaricoInfo, currentStatus, nextStatus);
        paperProgressStatusDto.setStatusDateTime(now());
        notificationTrackerQueueDto.setPaperProgressStatusDto(paperProgressStatusDto);
        return notificationTrackerQueueDto;
    }
}
