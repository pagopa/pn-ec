package it.pagopa.pn.ec.consolidatore.exception;

import java.util.List;

import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RicezioneEsitiCartaceoException extends RuntimeException {

	private static final long serialVersionUID = -8223262171336272462L;
	
	private final String resultCode;
	private final String resultDescription;
	private final List<String> errorList;
	private final List<ConsAuditLogError> auditLogErrorList;
	
	public RicezioneEsitiCartaceoException(String resultCode, String resultDescription, List<String> errorList, List<ConsAuditLogError> auditLogErrorList) {
		super(String.format("RicezioneEsitiCartaceo : resultCode = %s : resultDescription = %s : errorList = %s", 
				resultCode, resultDescription, errorList));
		this.resultCode = resultCode;
		this.resultDescription = resultDescription;
		this.errorList = errorList;
		this.auditLogErrorList = auditLogErrorList;
	}

}
