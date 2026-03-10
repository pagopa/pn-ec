package it.pagopa.pn.ec.commons.utils;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.ec.commons.exception.ses.SesEventException;
import it.pagopa.pn.ec.email.model.dto.ses.SesNotificationDto;
import lombok.CustomLog;

@CustomLog
public class SesEventsUtils {

    public static final String DELIVERED = "delivered";
    public static final String BOUNCED = "bounced";
    public static final String COMPLAINT = "spam";
    public static final String REJECTED = "rejected";
    public static final String BOUNCE_TYPE_PERMANENT = "Permanent";
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

    /**
     * Determina se un evento SES di tipo Bounce non permanente deve essere ignorato:
     * - se l'evento è un bounce non permanente, lo acknowledge immediatamente e ritorna true, skip del flusso
     * - se il bounce è permanente, ritorna false e il flusso continua
     */
    public static boolean chooseBounceType(SesNotificationDto sesNotificationDto, Acknowledgement acknowledgement, String eventType, String messageId) {
        boolean isNonPermanentBounce = BOUNCE_EVENT.equalsIgnoreCase(eventType) && !BOUNCE_TYPE_PERMANENT.equalsIgnoreCase(sesNotificationDto.getBounce().getBounceType());
        if (isNonPermanentBounce) {
            log.info("Ignorato bounce non permanente messageId={}", messageId);
            if (acknowledgement != null) {
                acknowledgement.acknowledge();
            }
        }
        return isNonPermanentBounce;
    }
}
