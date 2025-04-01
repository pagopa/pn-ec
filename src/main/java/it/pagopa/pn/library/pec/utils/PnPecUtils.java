package it.pagopa.pn.library.pec.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;


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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crea una stringa JSON con il formato Embedded Metric Format (EMF) di AWS CloudWatch per i log.
     * Utilizzato per i messaggi non letti della PEC.
     *
     * @param namespace  il namespace della metrica
     * @param metricName il nome della metrica
     * @param count      il valore della metrica (numero di messaggi non letti)
     * @return           una stringa JSON formattata per EMF
     *
     */
    public static String createEmfJson(String namespace, String metricName, Long count) {
        try {
            ObjectNode emfLog = objectMapper.createObjectNode();

            ObjectNode awsNode = objectMapper.createObjectNode();
            awsNode.put("Timestamp", Instant.now().toEpochMilli());

            ObjectNode metricsNode = objectMapper.createObjectNode();
            metricsNode.put("Namespace", namespace);

            ObjectNode metricDetails = objectMapper.createObjectNode();
            metricDetails.put("Name", metricName);
            metricDetails.put("Unit", "Count");

            metricsNode.set("Metrics", objectMapper.createArrayNode().add(metricDetails));

            awsNode.set("CloudWatchMetrics", objectMapper.createArrayNode().add(metricsNode));
            emfLog.set("_aws", awsNode);

            emfLog.put(metricName, count);

            return objectMapper.writeValueAsString(emfLog);
        } catch (Exception e) {
            throw new IllegalArgumentException("Errore nella creazione del JSON EMF", e);
        }
    }

}
