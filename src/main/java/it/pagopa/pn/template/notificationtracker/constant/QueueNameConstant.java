package it.pagopa.pn.template.notificationtracker.constant;

import java.util.List;

@SuppressWarnings("unused")
public class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

//  ###### QUEUE SECTION #####

    //  <-- SMS -->
    public static final String NT_STATO_SMS_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-queue";
    public static final String NT_STATO_SMS_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-queue-dlq";
    public static final String NT_STATO_SMS_ERRATO_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-errato-queue";
    public static final String NT_STATO_SMS_ERRATO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-sms-errato-queue-dlq";
    public static final String NT_STATO_EMAIL_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-queue";
    public static final String NT_STATO_EMAIL_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-queue-dlq";
    public static final String NT_STATO_EMAIL_ERRATO_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-errato-queue";
    public static final String NT_STATO_EMAIL_ERRATO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-email-errato-queue-dlq";
    public static final String NT_STATO_PEC_QUEUE_NAME = "pn-ec-notification-tracker-stato-pec-queue";
    public static final String NT_STATO_PEC_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-pec-queue-dlq";
    public static final String NT_STATO_PEC_ERRATO_QUEUE_NAME = "pn-ec-notification-tracker-stato-pec-errato-queue";
    public static final String NT_STATO_PEC_ERRATO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-pec-errato-queue-dlq";
    public static final String NT_STATO_CARTACEO_QUEUE_NAME = "pn-ec-notification-tracker-stato-cartaceo-queue";
    public static final String NT_STATO_CARTACEO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-cartaceo-queue-dlq";
    public static final String NT_STATO_CARTACEO_ERRATO_QUEUE_NAME = "pn-ec-notification-tracker-stato-cartaceo-errato-queue";
    public static final String NT_STATO_CARTACEO_ERRATO_DLQ_QUEUE_NAME = "pn-ec-notification-tracker-stato-cartaceo-errato-queue-dlq";



    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(NT_STATO_SMS_QUEUE_NAME,
                                                                   NT_STATO_SMS_DLQ_QUEUE_NAME,
                                                                   NT_STATO_SMS_ERRATO_QUEUE_NAME,
                                                                   NT_STATO_SMS_ERRATO_DLQ_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_DLQ_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_ERRATO_QUEUE_NAME,
                                                                   NT_STATO_EMAIL_ERRATO_DLQ_QUEUE_NAME,
                                                                   NT_STATO_PEC_QUEUE_NAME,
                                                                   NT_STATO_PEC_DLQ_QUEUE_NAME,
                                                                   NT_STATO_PEC_ERRATO_QUEUE_NAME,
                                                                   NT_STATO_PEC_ERRATO_DLQ_QUEUE_NAME,
                                                                   NT_STATO_CARTACEO_QUEUE_NAME,
                                                                   NT_STATO_CARTACEO_DLQ_QUEUE_NAME,
                                                                   NT_STATO_CARTACEO_ERRATO_QUEUE_NAME,
                                                                   NT_STATO_CARTACEO_ERRATO_DLQ_QUEUE_NAME
                                                                                                       );
}
