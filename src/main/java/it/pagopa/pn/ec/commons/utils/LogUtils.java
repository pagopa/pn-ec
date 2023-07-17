package it.pagopa.pn.ec.commons.utils;

public class LogUtils {
    public static final String INVALID_API_KEY = "Invalid API key";

    public static final String STARTING_PROCESS_LABEL = "Starting {} Process.";
    public static final String ENDING_PROCESS_LABEL = "Ending {} Process.";
    public static final String ENDING_PROCESS_WITH_ERROR_LABEL = "Ending {} Process with error= {} - {}";
    public static final String INVOKED_OPERATION_LABEL = "Invoked operationId {} with args: {}";
    public static final String SUCCESSFUL_OPERATION_LABEL="Successful API operation: {} = {}";
    public static final String SUCCESSFUL_OPERATION_NO_RESULT_LABEL="Successful API operation: {}";

    public static final String PRESA_IN_CARICO = "PresaInCaricoService.presaInCarico()";
    public static final String DIGITAL_PULL_SERVICE = "StatusPullService.digitalPullService()";


    //PEC
    public static final String GET_DIGITAL_LEGAL_MESSAGE_STATUS = "getDigitalLegalMessageStatus";
    public static final String SEND_DIGITAL_LEGAL_MESSAGE = "sendDigitalLegalMessage";
    public static final String PEC_PULL_SERVICE = "StatusPullService.pecPullService()";

    //PAPER
    public static final String SEND_PAPER_ENGAGE_REQUEST = "sendPaperEngageRequest";
    public static final String GET_PAPER_ENGAGE_PROGRESSES = "getPaperEngageProgresses";
    public static final String PAPER_PULL_SERVICE = "StatusPullService.paperPullService()";

    //EMAIL
    public static final String SEND_DIGITAL_COURTESY_MESSAGE = "sendDigitalCourtesyMessage";
    public static final String GET_DIGITAL_COURTESY_MESSAGE_STATUS = "getDigitalCourtesyMessageStatus";

    //SMS
    public static final String SEND_COURTESY_SHORT_MESSAGE = "sendCourtesyShortMessage";
    public static final String GET_COURTESY_SHORT_MESSAGE_STATUS = "getCourtesyShortMessageStatus";

}
