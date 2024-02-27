package it.pagopa.pn.library.pec.exception.aruba;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArubaCallException extends RuntimeException {

    private int errorCode;
    public ArubaCallException(String message) {
        super(message);
    }

    public ArubaCallException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

}
