package it.pagopa.pn.ec.constant;

public final class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

    public static final String NOTIFICATION_TRACKER_QUEUE_NAME = "pn-ec-notification-tracker-queue-temp";

    // TODO: To implement on AWS
    public static final String SMS_QUEUE_NAME = "pn-ec-sms-queue";

    // TODO: To implement on AWS
    public static final String SMS_ERROR_QUEUE_NAME = "pn-ec-sms-error-queue";
}
