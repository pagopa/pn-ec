package it.pagopa.pn.ec.exception;

public class IdClientNotFoundException extends RuntimeException {

    public IdClientNotFoundException(String idClient) {
        super(String.format("Client id %s is unauthorized", idClient));
    }
}