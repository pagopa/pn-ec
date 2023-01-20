package it.pagopa.pnec.notificationtracker.utils;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

public class QueueUtils {

    private QueueUtils() {
        throw new IllegalStateException("QueueUtils is a utility class");
    }

    public static String getQueueUrl(SqsClient sqsClient, String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }
}
