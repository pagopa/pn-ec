package it.pagopa.pn.ec.commons.utils;

public class LogUtils {
    public static final String INVALID_API_KEY = "Invalid API key";
    public static final String STARTING_PROCESS_LABEL = "Starting '{}' Process.";
    public static final String STARTING_PROCESS_ON_LABEL = "Starting '{}' Process on '{}'.";
    public static final String ENDING_PROCESS_LABEL = "Ending '{}' Process.";
    public static final String ENDING_PROCESS_ON_LABEL = "Ending '{}' Process on '{}'.";
    public static final String ENDING_PROCESS_WITH_ERROR_LABEL = "Ending '{}' Process with error = {} - {}";
    public static final String ENDING_PROCESS_ON_WITH_ERROR_LABEL = "Ending '{}' Process on '{}' with error = {} - {}";

    public static final String INVOKING_OPERATION_LABEL_WITH_ARGS = "Invoking operation '{}' with args: {}";
    public static final String INVOKING_OPERATION_LABEL = "Invoking operation '{}'";
    public static final String SUCCESSFUL_OPERATION_LABEL = "Successful operation: '{}' = {}";

    public static final String SUCCESSFUL_OPERATION_ON_LABEL = "Successful operation on '{}' : '{}' = {}";
    public static final String SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL = "Successful operation on '{}': '{}'";
    public static final String SUCCESSFUL_OPERATION_NO_RESULT_LABEL = "Successful operation: '{}'";
    public static final String INSERTING_DATA_IN_SQS = "Inserting data {} in SQS '{}'";
    public static final String INSERTED_DATA_IN_SQS = "Inserted data in SQS '{}'";
    public static final String PRESA_IN_CARICO = "PresaInCaricoService.presaInCarico()";
    public static final String MESSAGE_REMOVED_FROM_ERROR_QUEUE = "The message '{}' has been successfully handled and removed from the error queue '{}'";
    public static final String EXCEPTION_IN_PROCESS_FOR = "Exception in '{}' for request '{}' - {}, {}";
    public static final String EXCEPTION_IN_PROCESS = "Exception in '{}' - {}, {}";
    public static final String FATAL_IN_PROCESS_FOR = "* FATAL * in '{}' for request '{}' - {}, {}";
    public static final String FATAL_IN_PROCESS = "* FATAL * in '{}' - {}, {}";
    public static final String SHORT_RETRY_ATTEMPT = "Short retry attempt number '{}' caused by : {} - {}";
    public static final String RETRY_ATTEMPT = "{} - retry attempt number '{}' for request '{}'";
    public static final String ARUBA_SEND_EXCEPTION = "ArubaSendException occurred during lavorazione PEC for request '{}' - Errcode: {}, Errstr: {}, Errblock: {}";


    //VALIDATION
    public static final String CHECKING_VALIDATION_PROCESS = "Checking {}";
    public static final String CHECKING_VALIDATION_PROCESS_ON = "Checking {} on '{}'";
    public static final String VALIDATION_PROCESS_PASSED = "{} passed";
    public static final String VALIDATION_PROCESS_FAILED = "{} failed error = {}";
    public static final String CLIENT_AUTHENTICATION = "Client authentication";
    public static final String X_API_KEY_VALIDATION = "XApiKey validation";

    //REPOSITORY MANAGER
    public static final String GET_REQUEST = "getRequest";
    public static final String GET_REQUEST_OP = "RequestService.getRequest()";
    public static final String INSERT_REQUEST = "insertRequest";
    public static final String INSERT_REQUEST_OP = "RequestService.insertRequest()";
    public static final String PATCH_REQUEST = "patchRequest";
    public static final String PATCH_REQUEST_OP = "RequestService.patchRequest()";
    public static final String DELETE_REQUEST = "deleteRequest";
    public static final String DELETE_REQUEST_OP = "RequestService.deleteRequest()";
    public static final String GET_REQUEST_BY_MESSAGE_ID = "getRequestByMessageId";
    public static final String GET_REQUEST_BY_MESSAGE_ID_OP = "RequestService.getRequestByMessageId()";
    public static final String SET_MESSAGE_ID_IN_REQUEST_METADATA = "setMessageIdInRequestMetadata";
    public static final String SET_MESSAGE_ID_IN_REQUEST_METADATA_OP = "RequestService.setMessageIdInRequestMetadata()";

    public static final String GET_REQUEST_METADATA_OP = "RequestMetadataService.getRequestMetadata()";
    public static final String INSERT_REQUEST_METADATA_OP = "RequestMetadataService.insertRequestMetadata()";
    public static final String PATCH_REQUEST_METADATA_OP = "RequestMetadataService.patchRequestMetadata()";
    public static final String DELETE_REQUEST_METADATA_OP = "RequestMetadataService.deleteRequestMetadata()";

    public static final String GET_REQUEST_PERSONAL_OP = "RequestPersonalService.getRequestPersonal()";
    public static final String INSERT_REQUEST_PERSONAL_OP = "RequestPersonalService.insertRequestPersonal()";
    public static final String DELETE_REQUEST_PERSONAL_OP = "RequestPersonalService.deleteRequestPersonal()";

    public static final String GET_CONFIGURATIONS = "getConfigurations";
    public static final String GET_CONFIGURATIONS_OP = "RequestService.getConfigurations()";
    public static final String GET_CLIENT = "getClient";
    public static final String INSERT_CLIENT = "insertClient";
    public static final String UPDATE_CLIENT = "updateClient";
    public static final String DELETE_CLIENT = "deleteClient";
    public static final String GET_ALL_CLIENT = "getAllClient";

    //NOTIFICATION_TRACKER
    public static final String NT_HANDLE_REQUEST_STATUS_CHANGE = "NotificationTrackerService.handleRequestStatusChange()";
    public static final String NT_HANDLE_MESSAGE_FROM_ERROR_QUEUE = "NotificationTrackerService.handleMessageFromErrorQueue()";


    //PEC
    public static final String GET_DIGITAL_LEGAL_MESSAGE_STATUS = "getDigitalLegalMessageStatus";
    public static final String SEND_DIGITAL_LEGAL_MESSAGE = "sendDigitalLegalMessage";
    public static final String PEC_PULL_SERVICE = "StatusPullService.pecPullService()";
    public static final String FILTER_REQUEST_PEC = "PecService.filterRequestPec()";
    public static final String LAVORAZIONE_RICHIESTA_PEC = "PecService.lavorazioneRichiesta()";
    public static final String PRESA_IN_CARICO_PEC = "PecService.presaInCarico()";
    public static final String GESTIONE_RETRY_PEC = "PecService.gestioneRetry()";
    public static final String INSERT_REQUEST_FROM_PEC = "PecService.insertRequestFromPec()";
    public static final String PEC_GET_ATTACHMENTS = "PecService.getAttachments()";
    public static final String PEC_DOWNLOAD_ATTACHMENT = "PecService.downloadAttachment()";
    public static final String PEC_SEND_MAIL = "PecService.sendMail()";
    public static final String PEC_SEND_MESSAGE = "PecService.sendMessage()";
    public static final String PEC_SET_MESSAGE_ID_IN_REQUEST_METADATA_STEP = "PecService.setMessageIdInRequestMetadataStep()";
    public static final String NOTIFICATION_TRACKER_STEP_PEC = "PecService.notificationTrackerStep()";
    public static final String ARUBA_SEND_MAIL_STEP_PEC = "PecService.arubaSendMailStep()";

    //PAPER
    public static final String SEND_PAPER_ENGAGE_REQUEST = "sendPaperEngageRequest";
    public static final String GET_PAPER_ENGAGE_PROGRESSES = "getPaperEngageProgresses";
    public static final String PAPER_PULL_SERVICE = "StatusPullService.paperPullService()";
    public static final String INSERT_REQUEST_FROM_CARTACEO = "CartaceoService.insertRequestFromCartaceo()";
    public static final String LAVORAZIONE_RICHIESTA_CARTACEO = "CartaceoService.lavorazioneRichiesta()";
    public static final String FILTER_REQUEST_CARTACEO = "CartaceoService.filterRequestCartaceo()";
    public static final String GESTIONE_RETRY_CARTACEO = "CartaceoService.gestioneRetry()";
    public static final String PRESA_IN_CARICO_CARTACEO = "CartaceoService.presaInCarico()";
    public static final String PROCESS_WITH_ATTACH_RETRY = "CartaceoService.processWithAttachRetry()";
    public static final String PROCESS_ONLY_BODY_RETRY = "CartaceoService.processOnlyBodyRetry()";
    public static final String NOTIFICATION_TRACKER_STEP_CARTACEO = "CartaceoService.notificationTrackerStep()";
    public static final String CARTACEO_PUT_REQUEST_STEP = "CartaceoService.putRequestStep()";
    //EMAIL
    public static final String SEND_DIGITAL_COURTESY_MESSAGE = "sendDigitalCourtesyMessage";
    public static final String GET_DIGITAL_COURTESY_MESSAGE_STATUS = "getDigitalCourtesyMessageStatus";
    public static final String INSERT_REQUEST_FROM_EMAIL = "EmailService.insertRequestFromEmail()";
    public static final String LAVORAZIONE_RICHIESTA_EMAIL = "EmailService.lavorazioneRichiesta()";
    public static final String GESTIONE_RETRY_EMAIL = "EmailService.gestioneRetry()";
    public static final String FILTER_REQUEST_EMAIL = "EmailService.filterRequestEmail()";
    public static final String PRESA_IN_CARICO_EMAIL = "EmailService.presaInCarico()";
    public static final String NOTIFICATION_TRACKER_STEP_EMAIL = "EmailService.notificationTrackerStep()";
    public static final String EMAIL_SES_SEND_STEP = "EmailService.sesSendStep()";


    //SMS
    public static final String SEND_COURTESY_SHORT_MESSAGE = "sendCourtesyShortMessage";
    public static final String GET_COURTESY_SHORT_MESSAGE_STATUS = "getCourtesyShortMessageStatus";
    public static final String PRESA_IN_CARICO_SMS = "SmsService.presaInCarico()";
    public static final String INSERT_REQUEST_FROM_SMS = "SmsService.insertRequestFromSms()";
    public static final String LAVORAZIONE_RICHIESTA_SMS = "SmsService.lavorazioneRichiesta()";
    public static final String FILTER_REQUEST_SMS = "SmsService.filterRequestSms()";
    public static final String GESTIONE_RETRY_SMS = "SmsService.gestioneRetry()";
    public static final String NOTIFICATION_TRACKER_STEP_SMS = "SmsService.notificationTrackerStep()";
    public static final String SNS_SEND_STEP_SMS = "SmsService.snsSendStep()";

    //CONSOLIDATORE
    public static final String PRESIGNED_UPLOAD_REQUEST = "ConsolidatoreService.presignedUploadRequest()";
    public static final String CONSOLIDATORE_GET_FILE = "ConsolidatoreService.getFile()";
    public static final String SEND_PAPER_PROGRESS_STATUS_REQUEST = "sendPaperProgressStatusRequest";
    public static final String SEND_PAPER_REPLICAS_ENGAGEMENT_REQUEST = "sendPaperReplicasEngagementRequest";
    public static final String GET_PAPER_REPLICAS_PROGRESSES_REQUEST = "getPaperReplicasProgressesRequest";
    public static final String VERIFICA_ESITO_DA_CONSOLIDATORE = "RicezioneEsitiCartaceoService.verificaEsitoDaConsolidatore()";
    public static final String VERIFICA_ATTACHMENTS = "RicezioneEsitiCartaceoService.verificaEsitoDaConsolidatore()";
    public static final String VERIFICA_ERRORI_SEMANTICI = "RicezioneEsitiCartaceoService.verificaErroriSemantici()";
    public static final String PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER = "RicezioneEsitiCartaceoService.pubblicaEsitoCodaNotificationTracker()";
    public static final String PUBLISH_ON_QUEUE = "RicezioneEsitiCartaceoService.publishOnQueue()";


    //STATEMACHINE
    public static final String STATE_MACHINE_SERVICE = "pn-statemachinemanager";
    public static final String STATUS_DECODE = "statusDecode";
    public static final String STATUS_VALIDATION = "statusValidation";

    //SAFESTORAGE
    public static final String SAFE_STORAGE_SERVICE = "pn-safestorage";
    public static final String GET_ALLEGATI_PRESIGNED_URL_OR_METADATA = "AttachmentService.getAllegatiPresignedUrlOrMetadata()";

    //COMMONS
    public static final String GET_FILE = "getFile";
    public static final String POST_FILE = "postFile";
    public static final String DIGITAL_PULL_SERVICE = "StatusPullService.digitalPullService()";

    //MICROSERVIZI ESTERNI
    public static final String INVOKING_INTERNAL_SERVICE = "Invoking internal service {} - {} with args: {}. Waiting Sync response.";
    public static final String GESTORE_REPOSITORY_SERVICE = "repositorymanager";

    //MICROSERVIZI ESTERNI
    public static final String INVOKING_EXTERNAL_SERVICE = "Invoking external service {} {}. Waiting Sync response.";
    public static final String CONSOLIDATORE_SERVICE = "Consolidatore";

    //SERVIZI ESTERNI
    public static final String CLIENT_METHOD_INVOCATION_WITH_ARGS = "Client method {} - args: {}";
    public static final String CLIENT_METHOD_INVOCATION = "Client method {}";
    public static final String CLIENT_METHOD_RETURN = "Return client method: {} = {}";

    //ARUBA
    public static final String ARUBA_GET_MESSAGES = "ArubaCall.getMessages()";
    public static final String ARUBA_GET_MESSAGE_ID = "ArubaCall.getMessageId()";
    public static final String ARUBA_SEND_MAIL = "ArubaCall.sendMail()";
    public static final String ARUBA_GET_ATTACH = "ArubaCall.getAttach()";

    //SES
    public static final String SES_SEND_MAIL = "SesService.sendMail()";

    //SNS
    public static final String SNS_SEND = "SnsService.send()";

    //DYNAMODB
    public static final String INSERTING_DATA_IN_DYNAMODB_TABLE = "Inserting data {} in DynamoDB table '{}'";
    public static final String INSERTED_DATA_IN_DYNAMODB_TABLE = "Inserted data in DynamoDB table '{}'";
    public static final String UPDATING_DATA_IN_DYNAMODB_TABLE = "Updating data {} in DynamoDB table '{}'";
    public static final String UPDATED_DATA_IN_DYNAMODB_TABLE = "Updated data in DynamoDB table '{}'";
    public static final String DELETING_DATA_FROM_DYNAMODB_TABLE = "Deleting data {} in DynamoDB table '{}'";
    public static final String DELETED_DATA_FROM_DYNAMODB_TABLE = "Deleted data in DynamoDB table '{}'";

    //DOWNLOAD CALL
    public static final String DOWNLOAD_FILE = "DownloadCall.downloadFile()";
    public static final String REQUEST_METADATA_TABLE = "RequestMetadata";

    //SCARICAMENTO/LAVORAZIONE ESITI PEC
    public static final String STARTING_SCHEDULED = "Starting scheduled process '{}'";
    public static final String SCARICAMENTO_ESITI_PEC = "scaricamentoEsitiPecScheduler()";
    public static final String PROCESSING_PEC = "Processing PEC '{}' in '{}' for request '{}' ";
    public static final String BUILDING_PEC_QUEUE_PAYLOAD = "Building queue payload for PEC '{}' in '{}'";
    public static final String PEC_DISCARDED = "PEC '{}' discarded in '{}' - reason : {}";
    public static final String NOT_SENT_BY_US = "Not sent by us";
    public static final String LAVORAZIONE_ESITI_PEC = "LavorazioneEsitiPecService.lavorazioneEsitiPec()";
    public static final String GENERATE_LOCATION = "LavorazioneEsitiPecService.generateLocation()";
    public static final String LOCATION_GENERATED = "Location generated in '{}' for request '{}'";


    //CLOUD WATCH
    public static final String PUBLISH_CUSTOM_PEC_METRICS = "CloudWatchPecMetrics.publishCustomPecMetrics()";

    //EVENT BRIDGE
    public static final String EVENT_BRIDGE_PUT_EVENT_EXTERNAL = "EventBridge - PutEvents.putEventExternal()";

}
