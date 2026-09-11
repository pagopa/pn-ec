package it.pagopa.pn.ec.cartaceo.rest.error;

import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.consolidatore.utils.PaperResult;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice
@CustomLog
public class PaperProgressesErrorHandler {

    @ExceptionHandler(ConsolidatoreException.class)
    public ResponseEntity<OperationResultCodeResponse> handleConsolidatoreError(ConsolidatoreException exception) {
        OperationResultCodeResponse operationResult = exception.getResponse() != null ? exception.getResponse() : buildOperationResult(exception);
        HttpStatus status = Objects.equals(exception.getUpstreamStatusCode(), NOT_FOUND.value()) ? NOT_FOUND : BAD_GATEWAY;
        log.warn("Consolidatore returned {}, responding with {} : {}", exception.getUpstreamStatusCode(), status.value(), operationResult);
        return new ResponseEntity<>(operationResult, status);
    }

    private OperationResultCodeResponse buildOperationResult(ConsolidatoreException exception) {
        return new OperationResultCodeResponse().resultCode(PaperResult.INTERNAL_SERVER_ERROR_CODE)
                                                .resultDescription(exception.getMessage())
                                                .clientResponseTimeStamp(OffsetDateTime.now());
    }
}
