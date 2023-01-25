package it.pagopa.pn.template.notificationtracker.exception;

public class SqsConvertToJsonPayloadException extends SqsPublishException {

    public <T> SqsConvertToJsonPayloadException(T queuePayload) {
        super(String.format("The conversion to Json of %s to Json failed", queuePayload.getClass().getName()));
    }
}
