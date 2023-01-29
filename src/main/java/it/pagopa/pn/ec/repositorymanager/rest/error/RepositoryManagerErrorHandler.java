package it.pagopa.pn.ec.repositorymanager.rest.error;

import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ControllerAdvice
public class RepositoryManagerErrorHandler {

    @ExceptionHandler(RepositoryManagerException.IdClientAlreadyPresent.class)
    public final ResponseEntity<Problem> handleForbiddenIdClient(RepositoryManagerException.IdClientAlreadyPresent exception) {
        var problem = new Problem();
        problem.setStatus(FORBIDDEN.value());
        problem.setTitle("Client id already exists");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, FORBIDDEN);
    }

    @ExceptionHandler(RepositoryManagerException.IdClientNotFoundException.class)
    public final ResponseEntity<Problem> handleNotFoundIdClient(RepositoryManagerException.IdClientNotFoundException exception) {
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle("Client id doesn't exists");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }
}
