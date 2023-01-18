package it.pagopa.pn.ec.constant;

public final class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

    public static final String TEMP_QUEUE_NAME = "pn-ec-notification-tracker-queue-temp";
}
