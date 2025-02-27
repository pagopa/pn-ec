package it.pagopa.pn.ec.consolidatore.utils;

public class PaperConstant {

    private PaperConstant(){
    }

    public static final String SS_IN_URI = "safestorage://";
    public static final String UNRECOGNIZED_ERROR = "%s unrecognized : value = \"%s\"";
    public static final String UNRECOGNIZED_ERROR_NO_VALUE = "%s unrecognized";
    public static final String NOT_VALID = "%s is not valid : value = \"%s\"";
    public static final String NOT_VALID_FOR = "%s is not valid for %s: value = \"%s\"";
    public static final String NOT_VALID_FUTURE_DATE = "%s is in the future: value = \"%s\"";
    public static final String NOT_VALID_PAST_DATE = "%s is in the past: value = \"%s\"";
    public static final String NOT_FOUND = "%s not found. Value = \"%s\"";
    public static final String MISMATCH_ERROR = "%s does not match %s. Value = \"%s\"";

    public static final String LOG_VERIF_LABEL = "RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : ";
    public static final String LOG_PUB_LABEL = "RicezioneEsitiCartaceoServiceImpl.pubblicaEsitoCodaNotificationTracker() : ";

    public static final String STATUS_CODE_LABEL = "statusCode";
    public static final String PRODUCT_TYPE_LABEL = "productType";
    public static final String STATUS_DATE_TIME_LABEL = "statusDateTime";
    public static final String IUN_LABEL = "iun";
    public static final String REQUEST_ID_LABEL = "requestId";
    public static final String DELIVERY_FAILURE_CAUSE_LABEL = "deliveryFailureCause";
    public static final String ATTACHMENT_DOCUMENT_TYPE_LABEL = "attachment.documentType";
    public static final String ATTACHMENT_URI_LABEL = "attachment.uri";
    public static final String CLIENT_REQUEST_TIMESTAMP_LABEL = "clientRequestTimeStamp";


    public static final String DUPLICATED_EVENT = "DUPLICATED_EVENT";
}
