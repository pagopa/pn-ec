package it.pagopa.pn.ec.commons.constant;

import java.util.List;

@SuppressWarnings("unused")
public class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

    //  <-- SMS -->
    public static final String NT_STATO_SMS_QUEUE_NAME = "dgs-bing-ec-PnEcQueueTrackerStatoSMS-AdO0ryhZLfOm";
    public static final String NT_STATO_SMS_ERRATO_QUEUE_NAME = "dgs-bing-ec-PnEcQueueTrackerErroriSMS-H3JDsXTyKuuU";
    public static final String SMS_BATCH_QUEUE_NAME = "dgs-bing-ec-PnEcQueueBatchSMS-bCrLoKHoeuNY";
    public static final String SMS_INTERACTIVE_QUEUE_NAME = "dgs-bing-ec-PnEcQueueInteractiveSMS-GBsn34gQhhV8";
    public static final String SMS_ERROR_QUEUE_NAME = "dgs-bing-ec-PnEcQueueErroriSMS-UjHpNjbCbWER";

    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(NT_STATO_SMS_QUEUE_NAME,
                                                                   NT_STATO_SMS_ERRATO_QUEUE_NAME,
                                                                   SMS_BATCH_QUEUE_NAME,
                                                                   SMS_INTERACTIVE_QUEUE_NAME,
                                                                   SMS_ERROR_QUEUE_NAME);
}
