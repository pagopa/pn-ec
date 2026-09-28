package it.pagopa.pn.ec.commons.exception.cartaceo;

import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConsolidatoreException extends RuntimeException {
    private OperationResultCodeResponse response;
    private final Integer upstreamStatusCode;

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

        private static final String PERMANENT_EXCEPTION_MESSAGE = "Permanent exception when calling consolidatore: %s";

        public PermanentException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format(PERMANENT_EXCEPTION_MESSAGE, operationResultCodeResponse), operationResultCodeResponse);
        }

        public PermanentException(OperationResultCodeResponse operationResultCodeResponse, Integer upstreamStatusCode) {
            super(String.format(PERMANENT_EXCEPTION_MESSAGE, operationResultCodeResponse),
                  operationResultCodeResponse,
                  upstreamStatusCode);
        }

        public PermanentException(String message) {
            super(String.format(PERMANENT_EXCEPTION_MESSAGE, message));
        }

        public PermanentException(String message, Integer upstreamStatusCode) {
            super(String.format(PERMANENT_EXCEPTION_MESSAGE, message), null, upstreamStatusCode);
        }
    }

    public static class TemporaryException extends ConsolidatoreException {

        private static final String TEMPORARY_EXCEPTION_MESSAGE = "Temporary exception when calling consolidatore: %s";

        public TemporaryException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format(TEMPORARY_EXCEPTION_MESSAGE, operationResultCodeResponse), operationResultCodeResponse);
        }

        public TemporaryException(OperationResultCodeResponse operationResultCodeResponse, Integer upstreamStatusCode) {
            super(String.format(TEMPORARY_EXCEPTION_MESSAGE, operationResultCodeResponse),
                  operationResultCodeResponse,
                  upstreamStatusCode);
        }

        public TemporaryException(String message) {
            super(String.format(TEMPORARY_EXCEPTION_MESSAGE, message));
        }

        public TemporaryException(String message, Integer upstreamStatusCode) {
            super(String.format(TEMPORARY_EXCEPTION_MESSAGE, message), null, upstreamStatusCode);
        }
    }

    public static class RequestIdNotFoundException extends ConsolidatoreException {

        public RequestIdNotFoundException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format("Request id not found on consolidatore: %s", operationResultCodeResponse),
                  operationResultCodeResponse,
                  NOT_FOUND.value());
        }
    }

    @Getter
    public static class RateLimitedException extends ConsolidatoreException {

        private final transient Duration retryAfter;

        public RateLimitedException(OperationResultCodeResponse operationResultCodeResponse, Duration retryAfter) {
            super(String.format("Consolidatore is rate limiting: %s", operationResultCodeResponse),
                  operationResultCodeResponse,
                  TOO_MANY_REQUESTS.value());
            this.retryAfter = retryAfter;
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
