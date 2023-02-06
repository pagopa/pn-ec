package it.pagopa.pn.ec.commons.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class EcInternalEndpointHttpException extends RuntimeException {

    public EcInternalEndpointHttpException() {
        super("Ec internal endpoint call failed");
    }

    public EcInternalEndpointHttpException(String statusCode) {
        super(String.format("Ec internal endpoint call failed with status %s", statusCode));
    }
}
