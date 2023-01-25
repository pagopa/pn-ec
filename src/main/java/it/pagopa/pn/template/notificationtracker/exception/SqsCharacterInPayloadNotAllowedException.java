package it.pagopa.pn.template.notificationtracker.exception;

public class SqsCharacterInPayloadNotAllowedException extends SqsPublishException {

    public SqsCharacterInPayloadNotAllowedException(String queuePayload) {
        super(String.format("This Json payload contains characters outside the allowed set -> %s", queuePayload));
    }
}
