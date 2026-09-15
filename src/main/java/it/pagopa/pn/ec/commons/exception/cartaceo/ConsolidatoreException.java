package it.pagopa.pn.ec.commons.exception.cartaceo;

import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Duration;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConsolidatoreException extends RuntimeException {
    private OperationResultCodeResponse response;
    private Integer upstreamStatusCode;

    public ConsolidatoreException(String message, OperationResultCodeResponse response, Integer upstreamStatusCode) {
        super(message);
        this.response = response;
        this.upstreamStatusCode = upstreamStatusCode;
    }

    public ConsolidatoreException(String message, OperationResultCodeResponse response) {
        this(message, response, null);
    }

    public ConsolidatoreException(String message) {
        this(message, null, null);
    }

    public static class PermanentException extends ConsolidatoreException {

        public PermanentException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format("Permanent exception when calling consolidatore: %s", operationResultCodeResponse), operationResultCodeResponse);
        }

        public PermanentException(OperationResultCodeResponse operationResultCodeResponse, Integer upstreamStatusCode) {
            super(String.format("Permanent exception when calling consolidatore: %s", operationResultCodeResponse),
                  operationResultCodeResponse,
                  upstreamStatusCode);
        }

        public PermanentException(String message) {
            super(String.format("Permanent exception when calling consolidatore: %s", message));
        }

        public PermanentException(String message, Integer upstreamStatusCode) {
            super(String.format("Permanent exception when calling consolidatore: %s", message), null, upstreamStatusCode);
        }
    }

    public static class TemporaryException extends ConsolidatoreException {
        public TemporaryException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format("Temporary exception when calling consolidatore: %s", operationResultCodeResponse), operationResultCodeResponse);
        }

        public TemporaryException(OperationResultCodeResponse operationResultCodeResponse, Integer upstreamStatusCode) {
            super(String.format("Temporary exception when calling consolidatore: %s", operationResultCodeResponse),
                  operationResultCodeResponse,
                  upstreamStatusCode);
        }

        public TemporaryException(String message) {
            super(String.format("Temporary exception when calling consolidatore: %s", message));
        }

        public TemporaryException(String message, Integer upstreamStatusCode) {
            super(String.format("Temporary exception when calling consolidatore: %s", message), null, upstreamStatusCode);
        }
    }

    public static class RequestIdNotFoundException extends PermanentException {

        public RequestIdNotFoundException(OperationResultCodeResponse operationResultCodeResponse) {
            super(operationResultCodeResponse, NOT_FOUND.value());
        }
    }

    public static class RateLimitedException extends TemporaryException {

        private final transient Duration retryAfter;

        public RateLimitedException(OperationResultCodeResponse operationResultCodeResponse, Duration retryAfter) {
            super(operationResultCodeResponse, TOO_MANY_REQUESTS.value());
            this.retryAfter = retryAfter;
        }

        public Duration getRetryAfter() {
            return retryAfter;
        }
    }

    public static class CallTimeoutException extends ConsolidatoreException {

        public CallTimeoutException(String message) {
            super(String.format("Consolidatore did not answer in time: %s", message));
        }
    }

    public static class ConnectionFailedException extends ConsolidatoreException {

        public ConnectionFailedException(String message) {
            super(String.format("Consolidatore is not reachable: %s", message));
        }
    }

}
