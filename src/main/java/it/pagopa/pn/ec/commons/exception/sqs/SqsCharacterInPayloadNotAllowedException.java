package it.pagopa.pn.ec.commons.exception.sqs;

public class SqsCharacterInPayloadNotAllowedException extends SqsClientException {

    public SqsCharacterInPayloadNotAllowedException(String queuePayload) {
        super(String.format("This Json payload contains characters outside the allowed set -> %s", queuePayload));
    }
}
