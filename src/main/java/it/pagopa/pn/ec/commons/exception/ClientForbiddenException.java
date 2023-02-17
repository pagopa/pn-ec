package it.pagopa.pn.ec.commons.exception;

public class ClientForbiddenException extends RuntimeException {

    public ClientForbiddenException(String idClient) {
        super(String.format("Client id '%s' not found", idClient));
    }
}
