package it.pagopa.pn.ec.commons.utils;

public class LogUtils {
    public static final String INVALID_API_KEY = "Invalid API key";

    public static final String STARTING_PROCESS_LABEL = "Starting {} Process.";
    public static final String ENDING_PROCESS_LABEL = "Ending {} Process.";
    public static final String ENDING_PROCESS_WITH_ERROR_LABEL = "Ending {} Process with error= {} - {}";
    public static final String INVOKED_OPERATION_LABEL = "Invoked operationId {} with args: {}";
    public static final String INVOKED_OPERATION_LABEL_NO_ARGS = "Invoked operationId {}";
    public static final String SUCCESSFUL_OPERATION_LABEL = "Successful API operation: {} = {}";
    public static final String SUCCESSFUL_OPERATION_NO_RESULT_LABEL = "Successful API operation: {}";

    public static final String PRESA_IN_CARICO = "PresaInCaricoService.presaInCarico()";
    public static final String DIGITAL_PULL_SERVICE = "StatusPullService.digitalPullService()";

    //REPOSITORY MANAGER
    public static final String GET_REQUEST = "getRequest";
    public static final String INSERT_REQUEST = "insertRequest";
    public static final String PATCH_REQUEST = "patchRequest";
    public static final String DELETE_REQUEST = "deleteRequest";
    public static final String GET_REQUEST_BY_MESSAGE_ID = "getRequestByMessageId";
    public static final String SET_MESSAGE_ID_IN_REQUEST_METADATA = "setMessageIdInRequestMetadata";
    public static final String GET_CONFIGURATIONS = "getConfigurations";
    public static final String GET_CLIENT = "getClient";
    public static final String INSERT_CLIENT = "insertClient";
    public static final String UPDATE_CLIENT = "updateClient";
    public static final String DELETE_CLIENT = "deleteClient";
    public static final String GET_ALL_CLIENT = "getAllClient";



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

    //CONSOLIDATORE
    public static final String PRESIGNED_UPLOAD_REQUEST = "presignedUploadRequest";
    public static final String SEND_PAPER_PROGRESS_STATUS_REQUEST = "sendPaperProgressStatusRequest";
    public static final String VERIFICA_ESITO_DA_CONSOLIDATORE = "verificaEsitoDaConsolidatore";
    public static final String PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER = "pubblicaEsitoCodaNotificationTracker";

    //STATEMACHINE
    public static final String STATUS_DECODE = "statusDecode";
    public static final String STATUS_VALIDATION = "statusValidation";

    //COMMONS
    public static final String GET_FILE = "getFile";
    public static final String POST_FILE = "postFile";

    //MICROSERVIZI ESTERNI
    public static final String INVOKING_EXTERNAL_SERVICE = "Invoking external service {} {}. Waiting Sync response.";
    public static final String SAFE_STORAGE_SERVICE = "pn-safestorage";
    public static final String STATE_MACHINE_SERVICE = "pn-statemachinemanager";

    //SERVIZI ESTERNI
    public static final String CLIENT_METHOD_INVOCATION = "Client method {} - args: {}";
    public static final String CLIENT_METHOD_RETURN = "Return client method: {} = {}";

    //ARUBA
    public static final String ARUBA_GET_MESSAGES = "ArubaCall.getMessages()";
    public static final String ARUBA_GET_MESSAGE_ID = "ArubaCall.getMessageId()";
    public static final String ARUBA_SEND_MAIL = "ArubaCall.sendMail()";
    public static final String ARUBA_GET_ATTACH = "ArubaCall.getAttach()";

    //DYNAMODB
    public static final String GETTING_DATA_FROM_DYNAMODB_TABLE = "Getting data {} from DynamoDB table {}";
    public static final String GOT_DATA_FROM_DYNAMODB_TABLE = "Got data from DynamoDB table {}";
    public static final String INSERTING_DATA_IN_DYNAMODB_TABLE = "Inserting data {} in DynamoDB table {}";
    public static final String INSERTED_DATA_IN_DYNAMODB_TABLE = "Inserted data in DynamoDB table {}";
    public static final String UPDATING_DATA_IN_DYNAMODB_TABLE = "Updating data {} in DynamoDB table {}";
    public static final String UPDATED_DATA_IN_DYNAMODB_TABLE = "Updated data in DynamoDB table {}";
    public static final String DELETING_DATA_IN_DYNAMODB_TABLE = "Deleting data {} in DynamoDB table {}";
    public static final String DELETED_DATA_IN_DYNAMODB_TABLE = "Deleted data in DynamoDB table {}";
    public static final String PATCHING_DATA_IN_DYNAMODB_TABLE = "Patching data {} in DynamoDB table {}";
    public static final String PATCHED_DATA_IN_DYNAMODB_TABLE = "Patched data in DynamoDB table {}";

    public static final String REQUEST_METADATA_TABLE="RequestMetadata";
}
