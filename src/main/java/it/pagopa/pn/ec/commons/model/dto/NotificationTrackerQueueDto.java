package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import it.pagopa.pn.ec.sercq.model.pojo.SercqPresaInCaricoInfo;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class NotificationTrackerQueueDto extends PresaInCaricoInfo {

    String nextStatus;
    DigitalProgressStatusDto digitalProgressStatusDto;
    PaperProgressStatusDto paperProgressStatusDto;
    int retry;

    @SuppressWarnings("all")
    private static NotificationTrackerQueueDto createNotificationTrackerQueueDto(PresaInCaricoInfo presaInCaricoInfo, String nextStatus) {
        return NotificationTrackerQueueDto.builder()
                                          .requestIdx(presaInCaricoInfo.getRequestIdx())
                                          .xPagopaExtchCxId(presaInCaricoInfo.getXPagopaExtchCxId())
                                          .nextStatus(nextStatus)
                                          .build();
    }

    public static NotificationTrackerQueueDto createNotificationTrackerQueueDtoDigital(PresaInCaricoInfo presaInCaricoInfo,
                                                                                       String nextStatus,
                                                                                       DigitalProgressStatusDto digitalProgressStatusDto) {
        var notificationTrackerQueueDto = createNotificationTrackerQueueDto(presaInCaricoInfo, nextStatus);
        digitalProgressStatusDto.setEventTimestamp(now());
        notificationTrackerQueueDto.setDigitalProgressStatusDto(digitalProgressStatusDto);
        return notificationTrackerQueueDto;
    }

    public static NotificationTrackerQueueDto createNotificationTrackerQueueDtoSercq(PresaInCaricoInfo presaInCaricoInfo,
                                                                                     String nextStatus,
                                                                                     DigitalProgressStatusDto digitalProgressStatusDto) {
        var notificationTrackerQueueDto = createNotificationTrackerQueueDto(presaInCaricoInfo, nextStatus);

        String timestampSercq = ((SercqPresaInCaricoInfo) presaInCaricoInfo).getDigitalNotificationRequest().getReceiverDigitalAddress();
        String getTimestamp = getTimepstampFromDigitalRecieverAddress(timestampSercq);
        OffsetDateTime timestamp = OffsetDateTime.parse(getTimestamp);
        digitalProgressStatusDto.setEventTimestamp(timestamp);
        notificationTrackerQueueDto.setDigitalProgressStatusDto(digitalProgressStatusDto);

        return notificationTrackerQueueDto;
    }

    private static String getTimepstampFromDigitalRecieverAddress(String digitalRecieverAddress){
       return digitalRecieverAddress.split("timestamp=")[1];
    }
    public static NotificationTrackerQueueDto createNotificationTrackerQueueDtoPaper(PresaInCaricoInfo presaInCaricoInfo,
                                                                                     String nextStatus,
                                                                                     PaperProgressStatusDto paperProgressStatusDto) {
        var notificationTrackerQueueDto = createNotificationTrackerQueueDto(presaInCaricoInfo, nextStatus);
        paperProgressStatusDto.setStatusDateTime(now());
        notificationTrackerQueueDto.setPaperProgressStatusDto(paperProgressStatusDto);
        return notificationTrackerQueueDto;
    }

    public static NotificationTrackerQueueDto createNotificationTrackerQueueDtoRicezioneEsitiPaper(PresaInCaricoInfo presaInCaricoInfo,
                                                                                     String nextStatus,
                                                                                     PaperProgressStatusDto paperProgressStatusDto) {
        var notificationTrackerQueueDto = createNotificationTrackerQueueDto(presaInCaricoInfo, nextStatus);
        paperProgressStatusDto.setStatusDateTime(paperProgressStatusDto.getStatusDateTime());
        notificationTrackerQueueDto.setPaperProgressStatusDto(paperProgressStatusDto);
        return notificationTrackerQueueDto;
    }

}
