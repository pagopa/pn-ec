package it.pagopa.pn.ec.consolidatore.utils;

import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.constant.Status.SENT;

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
	public static final String DUPLICATED_EVENT_ERROR_DESCRIPTION = "Ricezione evento duplicato";

	// STATUS CODES
    public static final String OK_CODE= "200.00";
	public static final String SYNTAX_ERROR_CODE = "400.01";
	public static final String SEMANTIC_ERROR_CODE = "400.02";
	public static final String REQUEST_ID_ERROR_CODE = "404.00";
	public static final String DUPLICATED_EVENT_ERROR_CODE = "400.09";
	public static final String AUTHENTICATION_ERROR_CODE = "401.00";
	public static final String DUPLICATED_REQUEST_CODE = "409.00";
	public static final String INTERNAL_SERVER_ERROR_CODE = "500.00";
	
	private static final Map<String, String> errorCodeDescriptionMap = Map.ofEntries(
			Map.entry(SYNTAX_ERROR_CODE, SYNTAX_ERROR_DESCRIPTION),
			Map.entry(SEMANTIC_ERROR_CODE, SEMANTIC_ERROR_DESCRIPTION),
			Map.entry(DUPLICATED_EVENT_ERROR_CODE, DUPLICATED_EVENT_ERROR_DESCRIPTION),
			Map.entry(REQUEST_ID_ERROR_CODE, REQUEST_ID_ERROR_DESCRIPTION),
			Map.entry(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR)
			);

	public static final Map<String, String> CODE_TO_STATUS_MAP = Map.ofEntries
			(
					Map.entry(SYNTAX_ERROR_CODE, SYNTAX_ERROR),
					Map.entry(SEMANTIC_ERROR_CODE, SEMANTIC_ERROR),
					Map.entry(AUTHENTICATION_ERROR_CODE, AUTHENTICATION_ERROR),
					Map.entry(DUPLICATED_REQUEST_CODE, DUPLICATED_REQUEST),
					Map.entry(OK_CODE, SENT.getStatusTransactionTableCompliant())
			);

	public static final List<String> TO_ACK_STATUS_CODES = List.of(SYNTAX_ERROR_CODE, SEMANTIC_ERROR_CODE, DUPLICATED_REQUEST_CODE);
	public static final List<String> TO_DLQ_STATUS_CODES = List.of(AUTHENTICATION_ERROR_CODE, REQUEST_ID_ERROR_CODE);

	public static Map<String, String> errorCodeDescriptionMap() {
		return errorCodeDescriptionMap;
	}
	
}
