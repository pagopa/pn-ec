package it.pagopa.pn.ec.service;

public interface SqsService {

    void send(final String queueName,final String messagePayload);
}
