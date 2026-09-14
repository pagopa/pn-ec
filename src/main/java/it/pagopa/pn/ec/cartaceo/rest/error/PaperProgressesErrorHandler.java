package it.pagopa.pn.ec.cartaceo.rest.error;

import it.pagopa.pn.ec.cartaceo.rest.PaperProgressesApiController;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@ControllerAdvice(assignableTypes = PaperProgressesApiController.class)
@CustomLog
public class PaperProgressesErrorHandler {

    private static final String AUTHENTICATION_FAILED_TITLE = "Consolidatore authentication failed";
    private static final String RATE_LIMITED_TITLE = "Consolidatore rate limit reached";
    private static final String TIMEOUT_TITLE = "Consolidatore timeout";
    private static final String UNREACHABLE_TITLE = "Consolidatore unreachable";
    private static final String GENERIC_ERROR_TITLE = "Consolidatore error";

    private static final String AUTHENTICATION_FAILED_DETAIL = "The consolidatore rejected external-channel credentials";
    private static final String RATE_LIMITED_DETAIL = "The consolidatore is throttling external-channel requests";
    private static final String TIMEOUT_DETAIL = "The consolidatore did not answer within the expected time";
    private static final String UNREACHABLE_DETAIL = "Unable to establish a connection with the consolidatore";
    private static final String GENERIC_ERROR_DETAIL = "The consolidatore responded with status %s";
    private static final String NON_CONFORMING_DETAIL = "The consolidatore returned a non conforming response";

    @ExceptionHandler(ConsolidatoreException.RequestIdNotFoundException.class)
    public ResponseEntity<OperationResultCodeResponse> handleRequestIdNotFound(ConsolidatoreException.RequestIdNotFoundException exception) {
        log.info("Consolidatore does not know the requested requestId: {}", exception.getResponse());
        return ResponseEntity.status(NOT_FOUND).contentType(APPLICATION_JSON).body(exception.getResponse());
    }

    @ExceptionHandler(ConsolidatoreException.CallTimeoutException.class)
    public ResponseEntity<Problem> handleTimeout(ConsolidatoreException.CallTimeoutException exception) {
        log.error("Consolidatore timed out: {}", exception.getMessage());
        return problemResponse(GATEWAY_TIMEOUT, TIMEOUT_TITLE, TIMEOUT_DETAIL);
    }

    @ExceptionHandler(ConsolidatoreException.ConnectionFailedException.class)
    public ResponseEntity<Problem> handleConnectionFailure(ConsolidatoreException.ConnectionFailedException exception) {
        log.error("Consolidatore connection failure: {}", exception.getMessage());
        return problemResponse(BAD_GATEWAY, UNREACHABLE_TITLE, UNREACHABLE_DETAIL);
    }

    @ExceptionHandler(ConsolidatoreException.RateLimitedException.class)
    public ResponseEntity<Problem> handleRateLimited(ConsolidatoreException.RateLimitedException exception) {
        log.error("Consolidatore is rate limiting, retry after {} : {}", exception.getRetryAfter(), exception.getResponse());
        return ResponseEntity.status(SERVICE_UNAVAILABLE)
                             .header(RETRY_AFTER, String.valueOf(exception.getRetryAfter().toSeconds()))
                             .contentType(APPLICATION_PROBLEM_JSON)
                             .body(buildProblem(SERVICE_UNAVAILABLE, RATE_LIMITED_TITLE, RATE_LIMITED_DETAIL));
    }

    @ExceptionHandler(ConsolidatoreException.class)
    public ResponseEntity<Problem> handleConsolidatoreError(ConsolidatoreException exception) {
        Integer upstreamStatusCode = exception.getUpstreamStatusCode();

        if (isAuthenticationFailure(upstreamStatusCode)) {
            log.fatal("Consolidatore rejected external-channel credentials: {}", exception.getResponse());
            return problemResponse(BAD_GATEWAY, AUTHENTICATION_FAILED_TITLE, AUTHENTICATION_FAILED_DETAIL);
        }

        log.error("Consolidatore returned {} : {}", upstreamStatusCode, exception.getMessage());
        String detail = upstreamStatusCode != null ? String.format(GENERIC_ERROR_DETAIL, upstreamStatusCode) : NON_CONFORMING_DETAIL;
        return problemResponse(BAD_GATEWAY, GENERIC_ERROR_TITLE, detail);
    }

    private boolean isAuthenticationFailure(Integer upstreamStatusCode) {
        return upstreamStatusCode != null && (upstreamStatusCode == UNAUTHORIZED.value() || upstreamStatusCode == FORBIDDEN.value());
    }

    private ResponseEntity<Problem> problemResponse(HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status).contentType(APPLICATION_PROBLEM_JSON).body(buildProblem(status, title, detail));
    }

    private Problem buildProblem(HttpStatus status, String title, String detail) {
        var problem = new Problem();
        problem.setStatus(status.value());
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setTraceId(UUID.randomUUID().toString());
        return problem;
    }
}
