package it.pagopa.pn.ec.commons.exception.sms;

public class SmsRetryException extends RuntimeException {

    public SmsRetryException() {
        super("Max retries have been exceeded");
    }

}
