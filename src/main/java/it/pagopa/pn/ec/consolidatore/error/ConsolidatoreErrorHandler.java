package it.pagopa.pn.ec.consolidatore.error;

import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.consolidatore.exception.ClientNotAuthorizedOrFoundException;
import it.pagopa.pn.ec.consolidatore.exception.SemanticException;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebInputException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice(basePackages = "it.pagopa.pn.ec.consolidatore")
public class ConsolidatoreErrorHandler {

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<OperationResultCodeResponse> handleMissingHeader(ServerWebInputException ex) {
        var response = new OperationResultCodeResponse();
        response.setResultDescription("Syntax Error");
        response.setResultCode("400.01");
        response.setErrorList(List.of(ex.getReason()));
        response.setClientResponseTimeStamp(OffsetDateTime.now());
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

    @ExceptionHandler(SemanticException.class)
    public ResponseEntity<OperationResultCodeResponse> handleSemanticError(SemanticException ex) {
        var response = new OperationResultCodeResponse();
        response.setResultDescription("Semantic Error");
        response.setResultCode("400.02");
        response.setErrorList(List.of(ex.getMessage()));
        response.setClientResponseTimeStamp(OffsetDateTime.now());
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Problem> handleGenericStatus(WebClientResponseException ex) {
        var problem = new Problem();
        problem.setStatus(ex.getRawStatusCode());
        problem.setTitle(ex.getStatusText());
        problem.setDetail(ex.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, ex.getStatusCode());
    }

    @ExceptionHandler(ClientNotAuthorizedOrFoundException.class)
    public ResponseEntity<Problem> handleClientNotAuthorized(ClientNotAuthorizedOrFoundException ex) {
        var problem = new Problem();
        problem.setStatus(NOT_FOUND.value());
        problem.setTitle(NOT_FOUND.toString());
        problem.setDetail(ex.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, NOT_FOUND);
    }

}
