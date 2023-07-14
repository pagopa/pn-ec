package it.pagopa.pn.ec.consolidatore.exception;

import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class SyntaxException extends RuntimeException{

    private List<String> errorList;
    private List<ConsAuditLogError> auditLogErrorList;

    public SyntaxException() {
        super("Syntax Error");
    }

    public SyntaxException errorList(List<String> errorList) {
        this.errorList = errorList;
        return this;
    }

    public SyntaxException auditLogErrorList(List<ConsAuditLogError> auditLogErrorList) {
        this.auditLogErrorList = auditLogErrorList;
        return this;
    }

}
