package it.pagopa.pn.ec.pec.exception;

public class MessageIdException extends RuntimeException{

    public MessageIdException(String message) {
        super(message);
    }

    public static class EncodeMessageIdException extends MessageIdException{

        public EncodeMessageIdException() {
            super("An error occurred during messageId encoding");
        }
    }

    public static class DecodeMessageIdException extends MessageIdException{

        public DecodeMessageIdException() {
            super("An error occurred during messageId decoding");
        }
    }
}
