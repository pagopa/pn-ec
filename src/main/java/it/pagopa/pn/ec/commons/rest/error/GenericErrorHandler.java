package it.pagopa.pn.ec.commons.rest.error;

import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice
public class GenericErrorHandler {

    @ExceptionHandler(Generic400ErrorException.class)
    public final ResponseEntity<Problem> handleGeneric400Error(Generic400ErrorException exception) {
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle(exception.getTitle());
        problem.setDetail(exception.getDetails());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }
}
