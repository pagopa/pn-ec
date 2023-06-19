package it.pagopa.pn.ec.commons.exception;

import lombok.Getter;

@Getter
public class ClientNotAuthorizedException extends RuntimeException {

    private String idClient;
    public ClientNotAuthorizedException(String idClient) {
        super(String.format("Client id '%s' is unauthorized", idClient));
        this.idClient = idClient;
    }
}
