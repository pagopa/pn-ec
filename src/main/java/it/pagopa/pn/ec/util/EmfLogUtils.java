package it.pagopa.pn.ec.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
public class EmfLogUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger jsonLogger = LoggerFactory.getLogger("it.pagopa.pn.JsonLogger");


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
    public static final String SERVICE_CONSOLIDATORE = "Consolidatore";
    public static final String CONSOLIDATORE_METRIC_NAME = "ExecutionTimeResponse";

    // MetricType
    public static final String METRIC_TYPE_MESSAGECOUNT = "MessageCount";
    public static final String METRIC_TYPE_CONSOLIDATORE = "TrackingConsolidatore";


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
            if (dimensions != null) {
                dimensions.forEach(dimensionsArray::add);
            }

            metricsNode.set(METRICS, objectMapper.createArrayNode().add(metricDef));
            metricsNode.set(DIMENSIONS, objectMapper.createArrayNode().add(dimensionsArray));

            awsNode.set(CLOUDWATCH_METRICS, objectMapper.createArrayNode().add(metricsNode));
            root.set(AWS, awsNode);

            if (values != null) {
                values.forEach(root::putPOJO);
            }

            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new IllegalArgumentException("Errore nella creazione del JSON EMF", e);
        }
    }

    public static void trackMetricsConsolidatore(int statusCode, long elapsedTime) {
        try {
            // valori comuni delle dimensioni
            Map<String, Object> baseValues = Map.of(
                    SERVICE, SERVICE_CONSOLIDATORE,
                    METRIC_TYPE, METRIC_TYPE_CONSOLIDATORE,
                    CODE_HTTP, String.valueOf(statusCode)
            );

            List<String> dimensions = List.of(SERVICE, METRIC_TYPE, CODE_HTTP);

            // 1. Count
            jsonLogger.info(EmfLogUtils.createEmfLog(
                    SERVICE_CONSOLIDATORE, // namespace
                    "ApiCall",               // metricName
                    "Count",               // unit
                    dimensions,            // dimensioni
                    baseValues             // valori
            ));

            // 2. StatusCode
            jsonLogger.info(EmfLogUtils.createEmfLog(
                    SERVICE_CONSOLIDATORE,
                    "StatusCodeResponse",
                    "None",
                    dimensions,
                    baseValues
            ));

            // 3. Timing
            Map<String, Object> timingValues = new HashMap<>(baseValues);
            timingValues.put("Timing", elapsedTime);
            jsonLogger.info(EmfLogUtils.createEmfLog(
                    SERVICE_CONSOLIDATORE,
                    "ApiCallTiming",
                    "Milliseconds",
                    dimensions,
                    timingValues
            ));

        } catch (Exception e) {
            log.warn("Errore nella generazione log EMF consolidatore", e);
        }
    }

}
