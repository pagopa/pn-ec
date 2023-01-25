package it.pagopa.pn.ec.commons.exception.sqs;

public class SqsConvertToJsonPayloadException extends SqsPublishException {

    public <T> SqsConvertToJsonPayloadException(T queuePayload) {
        super(String.format("The conversion to Json of %s to Json failed", queuePayload.getClass().getName()));
    }
}
