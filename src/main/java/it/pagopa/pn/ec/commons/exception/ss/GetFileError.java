package it.pagopa.pn.ec.commons.exception.ss;

public class GetFileError extends Exception {

    public GetFileError(String fileKey, String xPagopaExtchCxId) {
        super(String.format("Error retrieving attachment '%s' by client '%s': check if the client id on Safe Storage exists, check" +
                            " that the client id has sufficient read permissions, check if the attachment exists",
                            fileKey,
                            xPagopaExtchCxId));
    }
}
