package it.pagopa.pn.ec.service;

public interface SqsService {

    <T> void send(final String queueName,final T queuePayload);
}
