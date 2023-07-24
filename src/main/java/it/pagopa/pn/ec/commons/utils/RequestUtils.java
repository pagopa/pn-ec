package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.repositorymanager.utils.RequestMapper;

public class RequestUtils {
    private static final String SEPARATORE = "~";

    private RequestUtils() {
        throw new IllegalStateException("RequestUtils is utility class");
    }


    public static String concatRequestId(String clientId, String requestId) {
        return (String.format("%s%s%s", clientId, SEPARATORE, requestId));
    }
}
