package it.pagopa.pn.ec.consolidatore.exception;

import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SemanticException extends RuntimeException {

    private List<String> errorList;
    private List<ConsAuditLogError> auditLogErrorList;

    public SemanticException() {
        super("Semantic Error");
    }

    public SemanticException errorList(List<String> errorList) {
        this.errorList = errorList;
        return this;
    }

    public SemanticException auditLogErrorList(List<ConsAuditLogError> auditLogErrorList) {
        this.auditLogErrorList = auditLogErrorList;
        return this;
    }
}
