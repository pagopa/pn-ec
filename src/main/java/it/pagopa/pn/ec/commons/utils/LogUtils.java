package it.pagopa.pn.ec.commons.utils;

public class LogUtils {

    private LogUtils() {
        throw new IllegalStateException("LogUtils is a utility class");
    }

    public static String concatIds(String id1, String id2) {
        return String.format("%s%s%s", id1, "~", id2);
    }

    public static final String MDC_CORR_ID_KEY = "cx_id";
    public static final String MDC_GUM_UUID_KEY = "get_unread_messages_uuid";
    public static final String INVALID_API_KEY = "Invalid API key";

    public static final String INVOKING_OPERATION_LABEL_WITH_ARGS = "Invoking operation '{}' with args: {}";

    public static final String LOGGING_OPERATION_WITH_ARGS = "Invoking operation '{}' with args: {} and {}";

    public static final String INVOKING_OPERATION_LABEL = "Invoking operation '{}'";
    public static final String SUCCESSFUL_OPERATION_LABEL = "Successful operation: '{}' = {}";

    public static final String SUCCESSFUL_OPERATION_ON_LABEL = "Successful operation on '{}' : '{}' = {}";
    public static final String SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL = "Successful operation on '{}': '{}'";
    public static final String SUCCESSFUL_OPERATION_NO_RESULT_LABEL = "Successful operation: '{}'";
    public static final String INSERTING_DATA_IN_SQS = "Inserting data {} in SQS '{}'";
    public static final String INSERTED_DATA_IN_SQS = "Inserted data in SQS '{}'";
    public static final String CHANGE_MESSAGE_VISIBILITY = "SqsService.changeMessageVisibility()";
    public static final String PRESA_IN_CARICO = "PresaInCaricoService.presaInCarico()";
    public static final String MESSAGE_REMOVED_FROM_ERROR_QUEUE = "The message has been successfully handled and removed from the error queue '{}'";
    public static final String EXCEPTION_IN_PROCESS_FOR = "Exception in '{}' for request '{}' - {}, {}";
    public static final String EXCEPTION_IN_PROCESS = "Exception in '{}' - {}, {}";
    public static final String SHORT_RETRY_ATTEMPT = "Short retry attempt number '{}' caused by : {} - {}";
    public static final String RETRY_ATTEMPT = "{} - retry attempt number '{}'";
    public static final String ARUBA_SEND_EXCEPTION = "ArubaSendException occurred during lavorazione PEC for request '{}' - Errcode: {}, Errstr: {}, Errblock: {}";
    public static final String ARUBA_MESSAGE_MISSING = "The message '{}' is missing from folder";
    public static final String GENERIC_ERROR = "Errore generico";

    //VALIDATION
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

    //NOTIFICATION TRACKER MESSAGE RECEIVER

    public static final String NT_RECEIVE_SMS = "receiveSMSObjectMessage()";

    public static final String NT_RECEIVE_SMS_ERROR = "receiveSMSObjectFromErrorQueue()";

    public static final String NT_RECEIVE_EMAIL = "receiveEmailObjectMessage()";

    public static final String NT_RECEIVE_EMAIL_ERROR = "receiveEmailObjectFromErrorQueue()";

    public static final String NT_RECEIVE_PEC = "receivePecObjectMessage()";

    public static final String NT_RECEIVE_PEC_ERROR = "receivePecObjectFromErrorQueue()";

    public static final String NT_RECEIVE_CARTACEO = "receiveCartaceoObjectMessage()";

    public static final String NT_RECEIVE_CARTACEO_ERROR = "receiveCartaceoObjectFromErrorQueue()";

    public static final String NT_RECEIVE_SERCQ = "receiveSercqObjectMessage()";

    public static final String NT_RECEIVE_SERCQ_ERROR = "receiveSercqObjectFromErrorQueue()";


    //PEC
    public static final String GET_DIGITAL_LEGAL_MESSAGE_STATUS = "getDigitalLegalMessageStatus";
    public static final String SEND_DIGITAL_LEGAL_MESSAGE = "sendDigitalLegalMessage";
    public static final String PEC_PULL_SERVICE = "StatusPullService.pecPullService()";
    public static final String FILTER_REQUEST_PEC = "PecService.filterRequestPec()";
    public static final String LAVORAZIONE_RICHIESTA_PEC = "lavorazioneRichiestaPec()";
    public static final String PRESA_IN_CARICO_PEC = "PecService.presaInCarico()";
    public static final String PRESA_IN_CARICO_SERCQ = "SercqService.presaInCarico()";
    public static final String GESTIONE_RETRY_PEC = "gestioneRetryPec()";
    public static final String INSERT_REQUEST_FROM_PEC = "PecService.insertRequestFromPec()";
    public static final String INSERT_REQUEST_FROM_SERCQ = "PecService.insertRequestFromSercq()";

    public static final String PEC_GET_ATTACHMENTS = "PecService.getAttachments()";
    public static final String PEC_DOWNLOAD_ATTACHMENT = "PecService.downloadAttachment()";
    public static final String PEC_SEND_MAIL = "PecService.sendMail()";
    public static final String PEC_SEND_MESSAGE = "PecService.sendMessage()";
    public static final String PEC_SET_MESSAGE_ID_IN_REQUEST_METADATA = "PecService.setMessageIdInRequestMetadata()";

    //PAPER
    public static final String SEND_PAPER_ENGAGE_REQUEST = "sendPaperEngageRequest";
    public static final String GET_PAPER_ENGAGE_PROGRESSES = "getPaperEngageProgresses";
    public static final String PAPER_PULL_SERVICE = "StatusPullService.paperPullService()";
    public static final String INSERT_REQUEST_FROM_CARTACEO = "CartaceoService.insertRequestFromCartaceo()";
    public static final String LAVORAZIONE_RICHIESTA_CARTACEO = "lavorazioneRichiestaCartaceo()";
    public static final String LAVORAZIONE_RICHIESTA_CARTACEO_BATCH = "lavorazioneRichiestaCartaceoBatch()";
    public static final String FILTER_REQUEST_CARTACEO = "CartaceoService.filterRequestCartaceo()";
    public static final String GESTIONE_RETRY_CARTACEO = "gestioneRetryCartaceo()";
    public static final String PRESA_IN_CARICO_CARTACEO = "CartaceoService.presaInCarico()";
    public static final String PROCESS_WITH_ATTACH_RETRY = "CartaceoService.processWithAttachRetry()";
    public static final String PROCESS_ONLY_BODY_RETRY = "CartaceoService.processOnlyBodyRetry()";
    public static final String UPLOAD_ATTACHMENT_TO_CONVERT = "CartaceoService.uploadAttachmentToConvert()";
    public static final String NOTIFICATION_TRACKER_STEP_CARTACEO = "CartaceoService.notificationTrackerStep()";
    public static final String CARTACEO_PUT_REQUEST_STEP = "CartaceoService.putRequestStep()";
    public static final String CARTACEO_PDF_RASTER_STEP = "CartaceoService.pdfRasterStep()";
    //EMAIL
    public static final String SEND_DIGITAL_COURTESY_MESSAGE = "sendDigitalCourtesyMessage";
    public static final String GET_DIGITAL_COURTESY_MESSAGE_STATUS = "getDigitalCourtesyMessageStatus";
    public static final String INSERT_REQUEST_FROM_EMAIL = "EmailService.insertRequestFromEmail()";
    public static final String LAVORAZIONE_RICHIESTA_EMAIL = "lavorazioneRichiestaEmail()";
    public static final String GESTIONE_RETRY_EMAIL = "gestioneRetryEmail()";
    public static final String FILTER_REQUEST_EMAIL = "EmailService.filterRequestEmail()";
    public static final String PRESA_IN_CARICO_EMAIL = "EmailService.presaInCarico()";

    //SMS
    public static final String SEND_COURTESY_SHORT_MESSAGE = "sendCourtesyShortMessage";
    public static final String GET_COURTESY_SHORT_MESSAGE_STATUS = "getCourtesyShortMessageStatus";
    public static final String PRESA_IN_CARICO_SMS = "SmsService.presaInCarico()";
    public static final String INSERT_REQUEST_FROM_SMS = "SmsService.insertRequestFromSms()";
    public static final String LAVORAZIONE_RICHIESTA_SMS = "lavorazioneRichiestaSms()";
    public static final String FILTER_REQUEST_SMS = "SmsService.filterRequestSms()";
    public static final String GESTIONE_RETRY_SMS = "gestioneRetrySms()";

    //CONSOLIDATORE
    public static final String PRESIGNED_UPLOAD_REQUEST_PROCESS = "presignedUploadRequest";
    public static final String PRESIGNED_UPLOAD_REQUEST = "ConsolidatoreService.presignedUploadRequest()";
    public static final String CONSOLIDATORE_GET_FILE = "ConsolidatoreService.getFile()";
    public static final String SEND_PAPER_PROGRESS_STATUS_REQUEST = "sendPaperProgressStatusRequest";
    public static final String SEND_PAPER_REPLICAS_ENGAGEMENT_REQUEST = "sendPaperReplicasEngagementRequest";
    public static final String GET_PAPER_REPLICAS_PROGRESSES_REQUEST = "getPaperReplicasProgressesRequest";
    public static final String VERIFICA_ESITO_DA_CONSOLIDATORE = "RicezioneEsitiCartaceoService.verificaEsitoDaConsolidatore()";
    public static final String VERIFICA_ATTACHMENTS = "RicezioneEsitiCartaceoService.verificaAttachments()";
    public static final String VERIFICA_ERRORI_SEMANTICI = "RicezioneEsitiCartaceoService.verificaErroriSemantici()";
    public static final String PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER = "RicezioneEsitiCartaceoService.pubblicaEsitoCodaNotificationTracker()";
    public static final String PUBLISH_ON_QUEUE = "RicezioneEsitiCartaceoService.publishOnQueue()";
    public static final String VERIFICA_DUPLICATI = "RicezioneEsitiCartaceoService.verificaDuplicati()";

    //PDF RASTER
    public static final String CONVERT_PDF = "convertPdf";

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
    public static final String CONSOLIDATORE_SERVICE = "Consolidatore";
    public static final String PDF_RASTER_SERVICE = "Pdf Raster";

    //SERVIZI ESTERNI
    public static final String CLIENT_METHOD_INVOCATION_WITH_ARGS = "Client method {} - args: {}";
    public static final String CLIENT_METHOD_INVOCATION = "Client method {}";
    public static final String CLIENT_METHOD_RETURN = "Return client method: {} = {}";
    public static final String CLIENT_METHOD_RETURN_WITH_ERROR = "Return client method '{}' with error: {} - {}";

    //ARUBA
    public static final String ARUBA_MARK_MESSAGE_AS_READ = "ArubaService.markMessageAsRead()";
    public static final String ARUBA_SEND_MAIL = "ArubaService.sendMail()";
    public static final String ARUBA_GET_MESSAGE_COUNT = "ArubaService.getMessageCount()";
    public static final String ARUBA_DELETE_MAIL = "ArubaService.deleteMail()";
    public static final String ARUBA_GET_UNREAD_MESSAGES = "ArubaService.getUnreadMessages()";
    public static final String INITIALIZING_ARUBA_PROXY_CLIENT = "Initializing Aruba proxy client for '{}' : ArubaServerAddress : {}, EndpointName : {}, ServiceName : {}";

    //SES
    public static final String SES_SEND_MAIL = "SesService.sendMail()";

    //SNS
    public static final String SNS_SEND = "SnsService.send()";

    //S3
    public static final String GET_OBJECT_AND_CONVERT = "getObjectAndConvert()";
    public static final String CONVERT_AND_PUT_OBJECT = "convertAndPutObject()";
    public static final String DELETE_OBJECT = "deleteObject()";

    //DOWNLOAD CALL
    public static final String DOWNLOAD_FILE = "DownloadCall.downloadFile()";
    public static final String UPLOAD_FILE = "UploadCall.uploadFile()";

    //SCARICAMENTO/LAVORAZIONE ESITI PEC
    public static final String STARTING_SCHEDULED = "Starting scheduled process '{}'";
    public static final String SCARICAMENTO_ESITI_PEC = "scaricamentoEsitiPecScheduler()";
    public static final String CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE = "cancellazioneRicevutePecInteractive()";
    public static final String CANCELLAZIONE_RICEVUTE_PEC = "CancellazioneRicevutePecService.cancellazioneRicevutePec()";
    public static final String PROCESSING_PEC = "Processing PEC '{}' in '{}' for request '{}' ";
    public static final String BUILDING_PEC_QUEUE_PAYLOAD = "Building queue payload for PEC '{}' in '{}'";
    public static final String PEC_DISCARDED = "PEC '{}' discarded in '{}' - reason : {}";
    public static final String NOT_SENT_BY_US = "Not sent by us";
    public static final String LAVORAZIONE_ESITI_PEC = "lavorazioneEsitiPec()";
    public static final String GENERATE_LOCATION = "LavorazioneEsitiPecService.generateLocation()";
    public static final String GET_MONO_MIME_MESSAGE = "EmailUtils.getMonoMimeMessage()";
    public static final String SET_ATTACHMENTS_IN_MIME_MESSAGE = "EmailUtils.setAttachmentsInMimeMessage()";
    public static final String SET_HEADERS_IN_MIME_MESSAGE = "EmailUtils.setHeadersInMimeMessage()";


    //CLOUD WATCH
    public static final String PUBLISH_CUSTOM_PEC_METRICS = "CloudWatchPecMetrics.publishCustomPecMetrics()";
    public static final String PUBLISH_PEC_MESSAGE_COUNT = "CloudWatchPecMetrics.publishMessageCount()";
    public static final String PUBLISH_RESPONSE_TIME = "CloudWatchPecMetrics.publishResponseTime()";
    public static final String GET_DIMENSION = "MetricsDimensionConfiguration.getDimension()";

    //EVENT BRIDGE
    public static final String EVENT_BRIDGE_PUT_EVENT_EXTERNAL = "EventBridge - PutEvents.putEventExternal()";

    //PN-PEC
    public static final String PN_PEC = "pn-pec";
    public static final String PN_EC_PEC_SEND_MAIL = "PnEcPecService.sendMail()";
    public static final String PN_EC_PEC_GET_UNREAD_MESSAGES = "PnEcPecService.getUnreadMessages()";
    public static final String PN_EC_PEC_GET_MESSAGE_COUNT = "PnEcPecService.getMessagesCount()";
    public static final String PN_EC_PEC_MARK_MESSAGE_AS_READ = "PnEcPecService.markMessageAsRead()";
    public static final String PN_EC_PEC_DELETE_MESSAGE = "PnEcPecService.deleteMessage()";
    public static final String NOT_VALID_FOR_DELETE = "Event with requestId '{}' is not valid for delete.";
    public static final String UNREAD_PEC_MESSAGE = "PnEcPecService.processAndLogUnreadPecMessages()";

    //CloudWatch

    public static final String CLOUD_WATCH_METRICS_PUBLISHER_WRAPPER = "CloudWatchMetricsPublisherWrapper";
    public static final String CLOUD_WATCH_METRICS_PUBLISH = CLOUD_WATCH_METRICS_PUBLISHER_WRAPPER + ".publish()";

    //PDFRASTER

    public static final String PDF_RASTER_INSERT_REQUEST_CONVERSION = "DynamoPdfRasterServiceImpl.insertRequestConversion()";
    public static final String PDF_RASTER_UPDATE_REQUEST_CONVERSION = "DynamoPdfRasterServiceImpl.updateRequestConversion()";
    public static final String PDF_RASTER_CONVERT_TO_ENTITY = "DynamoPdfRasterServiceImpl.convertToEntity()";
    public static final String PDF_RASTER_SAVE_REQUEST_CONVERSION_ENTITY = "DynamoPdfRasterServiceImpl.saveRequestConversionEntity()";
    public static final String PDF_RASTER_SAVE_PDF_CONVERSIONS = "DynamoPdfRasterServiceImpl.savePdfConversion()";
    public static final String PDF_RASTER_SAVE_PDF_CONVERSION = "DynamoPdfRasterServiceImpl.savePdfConversion()";
    public static final String PDF_RASTER_CONVERT_TO_DTO = "DynamoPdfRasterServiceImpl.convertToDto()";
    public static final String PDF_RASTER_UPDATE_ATTACHMENT_CONVERSION = "DynamoPdfRasterServiceImpl.updateAttachmentConversion()";
    public static final String PDF_RASTER_GET_REQUEST_CONVERSION_FROM_DYNAMO_DB = "DynamoPdfRasterServiceImpl.getRequestConversionFromDynamoDb()";
    public static final String PDF_RASTER_GET_PDF_CONVERSION_FROM_DYNAMO_DB = "DynamoPdfRasterServiceImpl.getPdfConversionFromDynamoDb()";
    public static final String HANDLE_AVAILABILITY_MANAGER = "AvailabilityManagerService.handleAvailabilityManager()";





    //SERCQ
    public static final String SEND_TO_SERCQ_SEND_SUCCESS = "OK, consegna da SeND a SERCQ-SeND conclusa con successo";

}
