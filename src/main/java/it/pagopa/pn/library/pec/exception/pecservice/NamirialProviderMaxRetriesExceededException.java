package it.pagopa.pn.library.pec.exception.pecservice;

public class NamirialProviderMaxRetriesExceededException extends MaxRetriesExceededException {


    public NamirialProviderMaxRetriesExceededException() {
        super("Namirial provider max retries exceeded");
    }

    public NamirialProviderMaxRetriesExceededException(String message) {
        super(message);
    }

    public NamirialProviderMaxRetriesExceededException(String message, Throwable cause) {
        super(message, cause);
    }

}
