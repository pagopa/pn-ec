package it.pagopa.pn.ec.commons.rest.error;

import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;

@ControllerAdvice
public class PresaInCaricoErrorHandler {

    @ExceptionHandler(RestCallException.ResourceAlreadyExistsException.class)
    public final ResponseEntity<Problem> handleRequestAlreadyInProgress(RestCallException.ResourceAlreadyExistsException exception) {
        var problem = new Problem();
        problem.setStatus(CONFLICT.value());
        problem.setTitle("Request already exists");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, CONFLICT);
    }
}
