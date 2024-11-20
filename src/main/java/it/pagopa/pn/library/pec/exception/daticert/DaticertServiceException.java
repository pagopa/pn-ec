package it.pagopa.pn.library.pec.exception.daticert;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DaticertServiceException extends RuntimeException {
    private String message;

    public DaticertServiceException(String message) {
        super(message);
    }

}
