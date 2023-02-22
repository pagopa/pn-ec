package it.pagopa.pn.ec.commons.exception;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String idClient) {
        super(String.format("Client id '%s' not found", idClient));
    }
}
