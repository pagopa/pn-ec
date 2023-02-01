package it.pagopa.pn.ec.commons.constant;

import java.util.List;

@SuppressWarnings("unused")
public class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

    //  <-- SMS -->
    public static final String NT_STATO_SMS_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-queue";
    public static final String NT_STATO_SMS_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-queue-dlq";
    public static final String NT_STATO_SMS_ERRATO_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-errato-queue";
    public static final String NT_STATO_SMS_ERRATO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-errato-queue-dlq";
    public static final String SMS_QUEUE_NAME = "pn-ec-sms-queue";
    public static final String SMS_DLQ_QUEUE_NAME = "pn-ec-sms-queue-dlq";
    public static final String SMS_ERROR_QUEUE_NAME = "pn-ec-sms-error-queue";
    public static final String SMS_ERROR_DLQ_QUEUE_NAME = "pn-ec-sms-errori-queue-dlq";


    //<-- EMAIL -->

    public static final String NT_STATO_EMAIL_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-queue";
    public static final String NT_STATO_EMAIL_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-queue-dlq";
    public static final String NT_STATO_EMAIL_ERRATO_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-errato-queue";
    public static final String NT_STATO_EMAIL_ERRATO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-errato-queue-dlq";
    public static final String EMAIL_QUEUE_NAME = "pn-ec-email-queue";
    public static final String EMAIL_DLQ_QUEUE_NAME = "pn-ec-email-queue-dlq";
    public static final String EMAIL_ERROR_QUEUE_NAME = "pn-ec-email-error-queue";
    public static final String EMAIL_ERROR_DLQ_QUEUE_NAME = "pn-ec-email-errori-queue-dlq";

    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(NT_STATO_SMS_QUEUE_NAME,
                                                                   NT_STATO_SMS_DLQ_QUEUE_NAME,
                                                                   NT_STATO_SMS_ERRATO_QUEUE_NAME,
                                                                   NT_STATO_SMS_ERRATO_DLQ_QUEUE_NAME,
                                                                   SMS_QUEUE_NAME,
                                                                   SMS_DLQ_QUEUE_NAME,
                                                                   SMS_ERROR_QUEUE_NAME,
                                                                   SMS_ERROR_DLQ_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_DLQ_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_ERRATO_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_ERRATO_DLQ_QUEUE_NAME,
                                                                   EMAIL_QUEUE_NAME,
                                                                   EMAIL_DLQ_QUEUE_NAME,
                                                                   EMAIL_ERROR_QUEUE_NAME,
                                                                   EMAIL_ERROR_DLQ_QUEUE_NAME);
}
