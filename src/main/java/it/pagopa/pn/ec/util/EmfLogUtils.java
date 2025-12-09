package it.pagopa.pn.ec.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class EmfLogUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Campi comuni
    public static final String AWS = "_aws";
    public static final String TIMESTAMP = "Timestamp";
    public static final String CLOUDWATCH_METRICS = "CloudWatchMetrics";
    public static final String NAMESPACE = "Namespace";
    public static final String METRICS = "Metrics";
    public static final String DIMENSIONS = "Dimensions";
    public static final String NAME = "Name";
    public static final String UNIT = "Unit";

    // Units
    public static final String UNIT_COUNT = "Count";
    public static final String UNIT_MILLISECONDS = "Milliseconds";

    // Dimension
    public static final String SERVICE = "Service";
    public static final String METRIC_TYPE = "MetricType";
    public static final String ELAPSED_TIME = "ElapsedTime";
    public static final String CODE_HTTP = "StatusResponse";

    // Service
    public static final String SERVICE_PEC = "PEC";
    public static final String SERVICE_CONSOLIDATORE = "PNConsolidatore";
    public static final String CONSOLIDATORE_METRIC_NAME = "ExecutionTimeResponse";

    // MetricType
    public static final String METRIC_TYPE_MESSAGECOUNT = "MessageCount";
    public static final String METRIC_TYPE_TIMING = "Timing";


    public static String createEmfLog(String namespace, String metricName, String unit, List<String> dimensions, Map<String, Object> values) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode awsNode = objectMapper.createObjectNode();
            awsNode.put(TIMESTAMP, Instant.now().toEpochMilli());

            ObjectNode metricsNode = objectMapper.createObjectNode();
            metricsNode.put(NAMESPACE, namespace);

            ObjectNode metricDef = objectMapper.createObjectNode();
            metricDef.put(NAME, metricName);
            metricDef.put(UNIT, unit);

            ArrayNode dimensionsArray = objectMapper.createArrayNode();
            dimensions.forEach(dimensionsArray::add);

            metricsNode.set(METRICS, objectMapper.createArrayNode().add(metricDef));
            metricsNode.set(DIMENSIONS, objectMapper.createArrayNode().add(dimensionsArray));

            awsNode.set(CLOUDWATCH_METRICS, objectMapper.createArrayNode().add(metricsNode));
            root.set(AWS, awsNode);

            values.forEach(root::putPOJO);

            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new IllegalArgumentException("Errore nella creazione del JSON EMF", e);
        }
    }
}
