package it.pagopa.pn.ec.testutils.constant;

public final class RestApiTestConstants {

    /*
    <-- Path param requestIdx section -->
    Regex on which it will be validated -> [0-9A-Za-z_-]{5,100}
     */
    public static final String DEFAULT_REQUEST_IDX = "mock_requestIdx-123";
    public static final String BAD_REQUEST_IDX_SHORT = "123";
    public static final String BAD_REQUEST_IDX_CHAR_NOT_ALLOWED = DEFAULT_REQUEST_IDX + ".";

    public static final String DEFAULT_ID_CLIENT_HEADER = "CLIENT_ID_123";
}
