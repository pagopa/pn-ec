package it.pagopa.pn.ec.commons.utils;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.ec.commons.exception.ses.SesEventException;
import it.pagopa.pn.ec.email.model.dto.ses.SesNotificationDto;
import lombok.CustomLog;
import reactor.core.publisher.Mono;

@CustomLog
public class SesEventsUtils {

    public static final String DELIVERED = "delivered";
    public static final String BOUNCED = "bounced";
    public static final String COMPLAINT = "spam";
    public static final String REJECTED = "rejected";
    //eventi
    public static final String BOUNCE_EVENT = "Bounce";
    public static final String DELIVERY_EVENT = "Delivery";
    public static final String COMPLAINT_EVENT = "Complaint";
    public static final String REJECT_EVENT = "Reject";


    public static String mapSesEventToStatus(String eventType) {
        return switch (eventType) {
            case DELIVERY_EVENT -> DELIVERED;
            case BOUNCE_EVENT -> BOUNCED;
            case COMPLAINT_EVENT -> COMPLAINT;
            case REJECT_EVENT -> REJECTED;
            default -> throw new SesEventException.EventTypeNotSupported(eventType);
        };
    }

    public static Mono<Boolean> preliminaryChecks(SesNotificationDto sesNotificationDto, Acknowledgement acknowledgement) {
        String messageId = sesNotificationDto.getMail() != null ? sesNotificationDto.getMail().getMessageId() : null;
        String eventType = sesNotificationDto.getNotificationType();

        if (messageId == null || messageId.isBlank()) {
            return Mono.error(new SesEventException.MessageIdNullOrEmpty());
        }
        if (eventType == null || eventType.isBlank()) {
            return Mono.error(new SesEventException.EventTypeNullOrEmpty(messageId));
        }
        return Mono.just(true); // tutto ok
    }
}
