package it.pagopa.pn.ec.commons.utils;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.ec.commons.exception.ses.SesEventException;
import it.pagopa.pn.ec.email.model.dto.ses.SesNotificationDto;
import it.pagopa.pn.ec.util.LogSanitizer;
import lombok.CustomLog;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private static final String EVENT_DETAILS_SEPARATOR = " - ";

    public static String mapSesEventToStatus(String eventType) {
        return switch (eventType) {
            case DELIVERY_EVENT -> DELIVERED;
            case BOUNCE_EVENT -> BOUNCED;
            case COMPLAINT_EVENT -> COMPLAINT;
            case REJECT_EVENT -> REJECTED;
            default -> throw new SesEventException.EventTypeNotSupported(eventType);
        };
    }

    /**
     * Costruisce il dettaglio dell'evento SES da inviare al notification tracker (eventDetails):
     * - Bounce: bounceType - bounceSubType
     * - Delivery: reportingMTA - smtpResponse (con eventuali indirizzi email mascherati)
     * - Reject: reason
     * - Complaint: complaintFeedbackType
     * Vengono riportate solo le parti valorizzate; se non ce n'è nessuna restituisce null.
     */
    public static String buildEventDetails(SesNotificationDto sesNotificationDto, LogSanitizer logSanitizer) {
        String eventType = sesNotificationDto.getNotificationType();
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case BOUNCE_EVENT -> Optional.ofNullable(sesNotificationDto.getBounce())
                    .map(bounce -> joinEventDetails(bounce.getBounceType(), bounce.getBounceSubType()))
                    .orElse(null);
            case DELIVERY_EVENT -> Optional.ofNullable(sesNotificationDto.getDelivery())
                    .map(delivery -> joinEventDetails(delivery.getReportingMTA(), logSanitizer.sanitizeEmail(delivery.getSmtpResponse())))
                    .orElse(null);
            case REJECT_EVENT -> Optional.ofNullable(sesNotificationDto.getReject())
                    .map(reject -> joinEventDetails(reject.getReason()))
                    .orElse(null);
            case COMPLAINT_EVENT -> Optional.ofNullable(sesNotificationDto.getComplaint())
                    .map(complaint -> joinEventDetails(complaint.getComplaintFeedbackType()))
                    .orElse(null);
            default -> null;
        };
    }

    private static String joinEventDetails(String... parts) {
        String joined = Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(EVENT_DETAILS_SEPARATOR));
        return joined.isEmpty() ? null : joined;
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
