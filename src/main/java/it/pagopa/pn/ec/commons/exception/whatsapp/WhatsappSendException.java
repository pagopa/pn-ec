package it.pagopa.pn.ec.commons.exception.whatsapp;

/**
 * Errore nell'invio di un messaggio WhatsApp tramite AWS End User Messaging Social.
 */
public class WhatsappSendException extends RuntimeException {

    public WhatsappSendException(String message) {
        super(message);
    }

    public WhatsappSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public WhatsappSendException() {
        super("An error occurred while sending via AWS End User Messaging Social (WhatsApp)");
    }

    public static class WhatsappMaxRetriesExceededException extends WhatsappSendException {

        public WhatsappMaxRetriesExceededException() {
            super("AWS End User Messaging Social (WhatsApp) max retries exceeded");
        }
    }
}
