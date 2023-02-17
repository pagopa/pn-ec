package it.pagopa.pn.ec.commons.exception.ss;

public class GetFileError extends Exception {

    public GetFileError(String fileKey, String xPagopaExtchCxId) {
        super(String.format("""
                                    Error retrieving attachment '%s' by client '%s'.
                                    1) Check if the client id on Safe Storage exists
                                    2) Check that the client id has sufficient read permissions
                                    3) Check if the attachment exists
                                    """, fileKey, xPagopaExtchCxId));
    }
}
