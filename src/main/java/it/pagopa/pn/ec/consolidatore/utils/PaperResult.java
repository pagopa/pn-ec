package it.pagopa.pn.ec.consolidatore.utils;

import it.pagopa.pn.ec.commons.constant.Status;

import java.util.Map;

public class PaperResult {

	private PaperResult() {
	}


	public static final String SYNTAX_ERROR = "syntaxError";
	public static final String SEMANTIC_ERROR = "semanticError";
	public static final String AUTHENTICATION_ERROR = "authenticationError";
	public static final String DUPLICATED_REQUEST = "duplicatedRequest";
	
	public static final String COMPLETED_OK_CODE = "200.00";
	public static final String COMPLETED_MESSAGE = "Accepted";
	public static final String INTERNAL_SERVER_ERROR = "Accepted";
	
	//
	public static final String SYNTAX_ERROR_DESCRIPTION = "Errore di validazione sintattica del messaggio";
	public static final String SEMANTIC_ERROR_DESCRIPTION = "Errore di validazione regole semantiche";
	public static final String REQUEST_ID_ERROR_DESCRIPTION = "requestId mai ricevuto";

	// ERROR CODES

	public static final String SYNTAX_ERROR_CODE = "400.01";
	public static final String SEMANTIC_ERROR_CODE = "400.02";
	public static final String REQUEST_ID_ERROR_CODE = "404.00";
	public static final String AUTHENTICATION_ERROR_CODE = "401.00";
	public static final String DUPLICATED_REQUEST_CODE = "409.00";
	public static final String INTERNAL_SERVER_ERROR_CODE = "500.00";
	
	private static final Map<String, String> errorCodeDescriptionMap = Map.ofEntries(
			Map.entry(SYNTAX_ERROR_CODE, SYNTAX_ERROR_DESCRIPTION),
			Map.entry(SEMANTIC_ERROR_CODE, SEMANTIC_ERROR_DESCRIPTION),
			Map.entry(REQUEST_ID_ERROR_CODE, REQUEST_ID_ERROR_DESCRIPTION),
			Map.entry(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR)
	);

	public static final Map<String, String> ERROR_TO_STATUS_MAP = Map.ofEntries
			(
					Map.entry(SYNTAX_ERROR_CODE, SYNTAX_ERROR),
					Map.entry(SEMANTIC_ERROR_CODE, SEMANTIC_ERROR),
					Map.entry(AUTHENTICATION_ERROR_CODE, AUTHENTICATION_ERROR),
					Map.entry(DUPLICATED_REQUEST_CODE, DUPLICATED_REQUEST)
			);


	public static Map<String, String> errorCodeDescriptionMap() {
		return errorCodeDescriptionMap;
	}
	
}
