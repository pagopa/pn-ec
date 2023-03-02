package it.pagopa.pn.ec.pec.exception;

public class MessageIdException extends RuntimeException{

    public MessageIdException(String message) {
        super(message);
    }

    public static class DecodeMessageIdException extends MessageIdException{

        public DecodeMessageIdException() {
            super("Decoding messageId error");
        }
    }
}
