package it.pagopa.pn.ec.commons.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqsUtils {

    private SqsUtils() {
        throw new IllegalStateException("SqsUtils is utility class");
    }

    public static <T> void logIncomingMessage(String queueName, T incomingPayload) {
        log.debug("Incoming message from '{}' queue with payload ↓\n{}", queueName, incomingPayload);
    }

    public static <T> void logIncomingMessage(String queueName, String id, T incomingPayload) {
        log.debug("Incoming message from '{}' queue with payload ↓\n{}", queueName, incomingPayload);
    }

}
