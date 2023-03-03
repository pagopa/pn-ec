package it.pagopa.pn.ec.pec.exception;

public class MessageIdException extends RuntimeException{

    public MessageIdException(String message) {
        super(message);
    }

    public static class EncodeMessageIdException extends MessageIdException{

        public EncodeMessageIdException() {
            super("Error while messageId encoding");
        }
    }

    public static class DecodeMessageIdException extends MessageIdException{

        public DecodeMessageIdException() {
            super("Error while messageId decoding");
        }
    }
}
