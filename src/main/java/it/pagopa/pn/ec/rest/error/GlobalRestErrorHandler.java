package it.pagopa.pn.ec.rest.error;

import it.pagopa.pn.ec.exception.IdClientNotFoundException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

@ControllerAdvice
public class GlobalRestErrorHandler {

    @ExceptionHandler(IdClientNotFoundException.class)
    public final ResponseEntity<Problem> handleUnauthorizedIdClient(IdClientNotFoundException exception) {
        Problem problem = new Problem();
        problem.setStatus(HttpStatus.UNAUTHORIZED.value());
        problem.setTitle("Client id unauthorized");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, HttpStatus.UNAUTHORIZED);
    }
}
