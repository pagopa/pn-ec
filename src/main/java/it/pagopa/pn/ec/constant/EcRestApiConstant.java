package it.pagopa.pn.ec.constant;

public final class EcRestApiConstant {

    private EcRestApiConstant() {
        throw new IllegalStateException("EcRestApiConstant is a constant class");
    }

    public static final String ID_CLIENT_HEADER = "x-pagopa-extch-cx-id";
    public static final String SEND_SMS_ENDPOINT = "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests/%s";
}