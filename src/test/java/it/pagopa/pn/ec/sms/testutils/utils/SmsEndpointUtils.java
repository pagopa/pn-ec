package it.pagopa.pn.ec.sms.testutils.utils;

public class SmsEndpointUtils {

    private SmsEndpointUtils() {
        throw new IllegalStateException("EndpointUtils is a utility class");
    }

    public static final String SEND_SMS_ENDPOINT = "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests/%s";

    public static String getSendSmsEndpoint(String requestIdx) {
        return String.format(SEND_SMS_ENDPOINT, requestIdx);
    }
}
