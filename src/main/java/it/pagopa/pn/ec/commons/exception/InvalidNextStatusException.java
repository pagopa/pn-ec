package it.pagopa.pn.ec.commons.exception;

public class InvalidNextStatusException extends RuntimeException {

    public InvalidNextStatusException(String currentStatus, String nextStatus, String processId, String xPagopaExtchCxId) {
        super(String.format("Status change from %s to %s is not valid for client %s within the process with id %s",
                            currentStatus,
                            nextStatus,
                            xPagopaExtchCxId,
                            processId));
    }
}
