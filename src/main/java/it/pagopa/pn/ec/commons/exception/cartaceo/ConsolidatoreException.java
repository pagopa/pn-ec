package it.pagopa.pn.ec.commons.exception.cartaceo;

import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConsolidatoreException extends RuntimeException {
    private OperationResultCodeResponse response;

    public ConsolidatoreException(String message, OperationResultCodeResponse response) {
        super(message);
        this.response = response;
    }

    public ConsolidatoreException(String message) {
        super(message);
    }

    public static class PermanentException extends ConsolidatoreException {

        public PermanentException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format("Permanent exception when calling consolidatore: %s", operationResultCodeResponse), operationResultCodeResponse);
        }

        public PermanentException(String message) {
            super(String.format("Permanent exception when calling consolidatore: %s", message));
        }
    }

    public static class TemporaryException extends ConsolidatoreException {
        public TemporaryException(OperationResultCodeResponse operationResultCodeResponse) {
            super(String.format("Temporary exception when calling consolidatore: %s", operationResultCodeResponse), operationResultCodeResponse);
        }

        public TemporaryException(String message) {
            super(String.format("Temporary exception when calling consolidatore: %s", message));
        }
    }

}
