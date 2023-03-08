package it.pagopa.pn.ec.cartaceo.exception;

public class RicezioneEsitiCartaceoException extends RuntimeException {

	private static final long serialVersionUID = -8223262171336272462L;
	
	private final String messageForResponse;

	public RicezioneEsitiCartaceoException(String message) {
		super(String.format("RicezioneEsitiCartaceo : errore = %s", message));
		this.messageForResponse = message;
	}
	
	public RicezioneEsitiCartaceoException(String message, String messageForResponse) {
		super(String.format("RicezioneEsitiCartaceo : errore = %s : messageForResponse = %s", message, messageForResponse));
		this.messageForResponse = messageForResponse;
	}

	public String getMessageForResponse() {
		return messageForResponse;
	}
	
}
