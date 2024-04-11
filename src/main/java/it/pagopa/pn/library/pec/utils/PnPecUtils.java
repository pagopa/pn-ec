package it.pagopa.pn.library.pec.utils;

public class PnPecUtils {

    private PnPecUtils() {
        throw new IllegalStateException("PnPecUtils is a utility class");
    }

    public static final String ARUBA_PROVIDER = "aruba";
    public static final String OTHER_PROVIDER = "other";
    public static final String SERVICE_ERROR = "Error retrieving messages from service: {}";
    public static final String RETRIES_EXCEEDED_MESSAGE = "Max retries exceeded for ";
    public static final String ARUBA_PROVIDER_SELECTED = "Aruba provider selected";
    public static final String OTHER_PROVIDER_SELECTED = "Other provider selected";
    public static final String ERROR_PARSING_PROPERTY_VALUES = "Error parsing property values, wrong value for service";
    public static final String ERROR_RETRIEVING_METRIC_NAMESPACE = "Error retrieving metric namespace. The given provider is not valid.";
    public static final String ARUBA_PATTERN_STRING = "@pec.aruba.it";

}
