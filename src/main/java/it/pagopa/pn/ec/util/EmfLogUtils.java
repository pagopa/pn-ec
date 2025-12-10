package it.pagopa.pn.ec.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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
    public static final String STATUS_CODE_RESPONSE = "StatusCodeResponse";
    public static final String API_CALL = "ApiCall";
    public static final String API_CALL_TIMING = "ApiCallTiming";

    // Service
    public static final String SERVICE_PEC = "PEC";
    public static final String SERVICE_CONSOLIDATORE = "Consolidatore";

    // MetricType
    public static final String METRIC_TYPE_MESSAGECOUNT = "MessageCount";


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
            List<String> dimensions = List.of(SERVICE);

            // JSON EMF con metriche separate e dimensione unica
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode awsNode = objectMapper.createObjectNode();
            awsNode.put(TIMESTAMP, Instant.now().toEpochMilli());

            ObjectNode metricsNode = objectMapper.createObjectNode();
            metricsNode.put(NAMESPACE, SERVICE_CONSOLIDATORE);

            ArrayNode metricsArray = objectMapper.createArrayNode();

            // 1. ApiCall
            ObjectNode metricCountApi = objectMapper.createObjectNode();
            metricCountApi.put(NAME, API_CALL);
            metricCountApi.put(UNIT, UNIT_COUNT);
            metricsArray.add(metricCountApi);

            // 2. StatusCodeResponse
            ObjectNode metricCodeResponse = objectMapper.createObjectNode();
            metricCodeResponse.put(NAME, STATUS_CODE_RESPONSE);
            metricCodeResponse.put(UNIT, UNIT_COUNT);
            metricsArray.add(metricCodeResponse);

            // 3. ApiCallTiming
            ObjectNode metricApiCallElapsed = objectMapper.createObjectNode();
            metricApiCallElapsed.put(NAME, API_CALL_TIMING);
            metricApiCallElapsed.put(UNIT, UNIT_MILLISECONDS);
            metricsArray.add(metricApiCallElapsed);

            metricsNode.set(METRICS, metricsArray);

            // dimensioni fisse
            ArrayNode dimensionsArray = objectMapper.createArrayNode();
            for (String dim : dimensions) {
                dimensionsArray.add(dim);
            }
            metricsNode.set(DIMENSIONS, objectMapper.createArrayNode().add(dimensionsArray));

            awsNode.set(CLOUDWATCH_METRICS, objectMapper.createArrayNode().add(metricsNode));
            root.set(AWS, awsNode);

            // valori metriche
            root.put(API_CALL, 1);
            root.put(STATUS_CODE_RESPONSE, statusCode);
            root.put(API_CALL_TIMING, elapsedTime);
            root.put(SERVICE, SERVICE_CONSOLIDATORE);

            jsonLogger.info(objectMapper.writeValueAsString(root));

        } catch (Exception e) {
            log.warn("Errore nella generazione log EMF consolidatore", e);
        }
    }
}
