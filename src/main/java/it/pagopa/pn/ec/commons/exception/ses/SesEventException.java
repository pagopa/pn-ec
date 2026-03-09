package it.pagopa.pn.ec.commons.exception.ses;

public class SesEventException extends RuntimeException {

    public SesEventException() {
        super("Generic exception in SES event processing");
    }

    public SesEventException(String message) {
        super(message);
    }

    public static class MessageIdNullOrEmpty extends SesEventException {
        public MessageIdNullOrEmpty() {
            super("MessageId from SES payload is null or empty");
        }
    }

    public static class EventTypeNullOrEmpty extends SesEventException {
        public EventTypeNullOrEmpty(String messageId) {
            super(String.format("NotificationType SES null or empty for messageId='%s'", messageId));
        }
    }

    public static class EventTypeNotSupported extends SesEventException {
        public EventTypeNotSupported(String eventType) {
            super(String.format("Evento SES not supported: '%s'", eventType));
        }
    }
}