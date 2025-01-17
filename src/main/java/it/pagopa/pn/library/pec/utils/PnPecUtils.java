package it.pagopa.pn.library.pec.utils;

public class PnPecUtils {

    private PnPecUtils() {
        throw new IllegalStateException("PnPecUtils is a utility class");
    }

    public static final String ARUBA_PROVIDER = "aruba";
    public static final String NAMIRIAL_PROVIDER = "namirial";
    public static final String DUMMY_PROVIDER = "dummy";
    public static final String SERVICE_ERROR = "Error retrieving messages from service: {}";
    public static final String RETRIES_EXCEEDED_MESSAGE = "Max retries exceeded for ";
    public static final String ARUBA_PROVIDER_SELECTED = "Aruba provider selected";
    public static final String NAMIRIAL_PROVIDER_SELECTED = "Namirial provider selected";
    public static final String DUMMY_PROVIDER_SELECTED = "Dummy provider selected";
    public static final String ERROR_PARSING_PROPERTY_VALUES = "Error parsing property values, wrong value for service";
    public static final String ERROR_RETRIEVING_METRIC_NAMESPACE = "Error retrieving metric namespace. The given provider is not valid.";
    public static final String ARUBA_PATTERN_STRING = "@pec.aruba.it";
    public static final String NAMIRIAL_PATTERN_STRING = "@sicurezzapostale.it";
    public static final String DUMMY_PATTERN_STRING = "@pec.dummy.it";
    public static final String DUMMY_PROVIDER_NAMESPACE = "dummy";

}
