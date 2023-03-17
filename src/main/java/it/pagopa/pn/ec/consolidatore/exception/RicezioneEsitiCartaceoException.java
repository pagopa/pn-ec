package it.pagopa.pn.ec.consolidatore.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class RicezioneEsitiCartaceoException extends RuntimeException {

	private static final long serialVersionUID = -8223262171336272462L;
	
	private final String resultCode;
	private final String resultDescription;
	private final List<String> errorList;

	public RicezioneEsitiCartaceoException(String message) {
		super(String.format("RicezioneEsitiCartaceo : errore = %s", message));
		this.resultCode = null;
		this.resultDescription = null;
		this.errorList = null;
	}
	
	public RicezioneEsitiCartaceoException(String resultCode, String resultDescription) {
		super(String.format("RicezioneEsitiCartaceo : resultCode = %s : resultDescription = %s", 
				resultCode, resultDescription));
		this.resultCode = resultCode;
		this.resultDescription = resultDescription;
		this.errorList = null;
	}
	
	public RicezioneEsitiCartaceoException(String resultCode, String resultDescription, List<String> errorList) {
		super(String.format("RicezioneEsitiCartaceo : resultCode = %s : resultDescription = %s : errorList = %s", 
				resultCode, resultDescription, errorList));
		this.resultCode = resultCode;
		this.resultDescription = resultDescription;
		this.errorList = errorList;
	}

}
