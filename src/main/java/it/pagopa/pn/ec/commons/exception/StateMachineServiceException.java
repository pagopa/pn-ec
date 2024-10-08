package it.pagopa.pn.ec.commons.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StateMachineServiceException extends RuntimeException {

    private HttpStatus httpStatus;

    public StateMachineServiceException(String message) {
        super("Exception during state machine service call: " + message);
    }

    public StateMachineServiceException(String message, Throwable cause) {
        super("Exception during state machine service call: " + message);
        this.initCause(cause);
    }

}