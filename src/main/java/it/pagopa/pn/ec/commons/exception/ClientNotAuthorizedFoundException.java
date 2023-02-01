package it.pagopa.pn.ec.commons.exception;

public class ClientNotAuthorizedFoundException extends RuntimeException {

    public ClientNotAuthorizedFoundException(String idClient) {
        super(String.format("Client id '%s' is unauthorized", idClient));
    }
}
