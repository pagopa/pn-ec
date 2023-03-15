package it.pagopa.pn.ec.commons.exception;

import it.pagopa.pn.ec.commons.model.pojo.RequestStatusChange;

public class InvalidNextStatusException extends RuntimeException {

    public InvalidNextStatusException(RequestStatusChange requestStatusChange) {
        super(String.format("Status change from %s to %s is not valid for client %s within the process with id %s",
                            requestStatusChange.getCurrentStatus(),
                            requestStatusChange.getNextStatus(),
                            requestStatusChange.getXPagopaExtchCxId(),
                            requestStatusChange.getProcessId()));
    }
}
