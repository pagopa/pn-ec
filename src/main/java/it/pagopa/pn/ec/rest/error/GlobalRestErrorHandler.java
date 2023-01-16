package it.pagopa.pn.ec.rest.error;

import it.pagopa.pn.ec.exception.IdClientNotFoundException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.rest.v1.dto.ProblemError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
@Slf4j
public class GlobalRestErrorHandler {

    private static final String DEFAULT_PROBLEM_ERROR_MESSAGE = "Internal error codes to be defined";

    @ExceptionHandler(IdClientNotFoundException.class)
    public final ResponseEntity<Problem> handleUnauthorizedIdClient(IdClientNotFoundException exception) {
        Problem problem = new Problem();
        problem.setStatus(UNAUTHORIZED.value());
        problem.setTitle("Client id unauthorized");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, UNAUTHORIZED);
    }

    @ExceptionHandler({WebExchangeBindException.class, ConstraintViolationException.class})
    public final ResponseEntity<Problem> handleBadRequest(Exception exception) {
        Problem problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle("Bad request");
        problem.setDetail("Check the errors array");
        problem.setTraceId(UUID.randomUUID().toString());
        if (exception instanceof WebExchangeBindException webExchangeBindException) {
            problem.setErrors(webExchangeBindException.getFieldErrors().stream().map(fieldError -> {
                ProblemError problemError = new ProblemError();
                problemError.setCode(DEFAULT_PROBLEM_ERROR_MESSAGE);
                problemError.setElement(fieldError.getField());
                problemError.setDetail(fieldError.getDefaultMessage());
                return problemError;
            }).toList());
        } else if (exception instanceof ConstraintViolationException constraintViolationException) {
            problem.setErrors(constraintViolationException.getConstraintViolations().stream().map(constraintViolation -> {
                ProblemError problemError = new ProblemError();
                problemError.setCode(DEFAULT_PROBLEM_ERROR_MESSAGE);
                String field = null;
                for (Path.Node node : constraintViolation.getPropertyPath()) {
                    field = node.getName();
                }
                problemError.setElement(field);
                problemError.setDetail(constraintViolation.getMessage());
                return problemError;
            }).toList());
        }
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }
}
