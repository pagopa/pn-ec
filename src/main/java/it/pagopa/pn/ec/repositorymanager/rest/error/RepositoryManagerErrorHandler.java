package it.pagopa.pn.ec.repositorymanager.rest.error;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
public class RepositoryManagerErrorHandler {

    @ExceptionHandler({RepositoryManagerException.IdClientAlreadyPresent.class, RepositoryManagerException.IdRequestAlreadyPresent.class})
    public final ResponseEntity<Problem> handleConflictIdClient(Exception exception) {
        var problem = new Problem();
        problem.setStatus(CONFLICT.value());
        problem.setTitle("Resource already exists");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, CONFLICT);
    }

    @ExceptionHandler({RepositoryManagerException.IdClientNotFoundException.class,
            RepositoryManagerException.RequestNotFoundException.class})
    public final ResponseEntity<Problem> handleNotFoundIdClient(Exception exception) {
        var problem = new Problem();
        problem.setStatus(NOT_FOUND.value());
        problem.setTitle("Resource not found");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, NOT_FOUND);
    }

    @ExceptionHandler(RepositoryManagerException.RequestMalformedException.class)
    public final ResponseEntity<Problem> handleRequestMalformed(RepositoryManagerException.RequestMalformedException exception) {
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle("Provided request malformed");
        problem.setDetail(exception.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }
}
