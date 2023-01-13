package it.pagopa.pn.ec.utils;

import static it.pagopa.pn.ec.constant.EcRestApiConstant.SEND_SMS_ENDPOINT;

public class EndpointUtils {

    private EndpointUtils() {
        throw new IllegalStateException("EndpointUtils is a utility class");
    }

    public static String getSendSmsEndpoint (String requestIdx){
     return String.format(SEND_SMS_ENDPOINT, requestIdx);
    }
}
