package it.pagopa.pn.library.pec.exception.pecservice;

public class AlternativeProviderMaxRetriesExceededException extends MaxRetriesExceededException {


    public AlternativeProviderMaxRetriesExceededException() {
        super("Alternative provider max retries exceeded");
    }

    public AlternativeProviderMaxRetriesExceededException(String message) {
        super(message);
    }

    public AlternativeProviderMaxRetriesExceededException(String message, Throwable cause) {
        super(message, cause);
    }

}
