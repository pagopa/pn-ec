package it.pagopa.pn.ec.consolidatore.utils;

import java.util.Map;

public class PaperResult {

	private PaperResult() {
	}
	
	public static final String COMPLETED_OK_CODE = "200.00";
	public static final String COMPLETED_MESSAGE = "Accepted";
	
	public static final String SYNTAX_ERROR_CODE = "400.01";
	public static final String SEMANTIC_ERROR_CODE = "400.02";
	public static final String REQUEST_ID_ERROR_CODE = "404.00";
	
	public static final String INTERNAL_SERVER_ERROR_CODE = "500.00";
	public static final String INTERNAL_SERVER_ERROR = "Accepted";
	
	public static final String SYNTAX_ERROR = "Errore di validazione sintattica del messaggio";
	public static final String SEMANTIC_ERROR = "Errore di validazione regole semantiche";
	public static final String REQUEST_ID_ERROR = "requestId mai ricevuto";
	
	private static final Map<String, String> errorCodeDescriptionMap = Map.ofEntries(
			Map.entry(SYNTAX_ERROR_CODE,SYNTAX_ERROR),
			Map.entry(SEMANTIC_ERROR_CODE,SEMANTIC_ERROR),
			Map.entry(REQUEST_ID_ERROR_CODE,REQUEST_ID_ERROR),
			Map.entry(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR)
	);

	public static Map<String, String> errorCodeDescriptionMap() {
		return errorCodeDescriptionMap;
	}
	
}
