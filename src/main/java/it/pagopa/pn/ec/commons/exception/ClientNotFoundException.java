package it.pagopa.pn.ec.commons.exception;

import lombok.Getter;

@Getter
public class ClientNotFoundException extends RuntimeException {

    private final String idClient;
    public ClientNotFoundException(String idClient) {
        super(String.format("Client id '%s' not found", idClient));
        this.idClient = idClient;
    }
}
