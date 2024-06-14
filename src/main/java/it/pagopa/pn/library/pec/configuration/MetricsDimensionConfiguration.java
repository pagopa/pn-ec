package it.pagopa.pn.library.pec.configuration;

import it.pagopa.pn.library.pec.configurationproperties.PnPecMetricNames;
import it.pagopa.pn.library.pec.utils.MetricsDimensionParser;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

/**
 * A configuration class for metrics dimensions. It prepares objects to handle metrics dimensions.
 */
@CustomLog
@Configuration
public class MetricsDimensionConfiguration {

    private final SsmClient ssmClient;
    private final MetricsDimensionParser metricsDimensionParser;
    private final PnPecMetricNames pnPecMetricNames;
    private Map<String, Map<String, List<Long>>> dimensionsSchema = new HashMap<>();
    @Value("${pn.pec.dimension.metrics.schema}")
    private String pnPecDimensionsSchemaName;

    public MetricsDimensionConfiguration(SsmClient ssmClient, PnPecMetricNames pnPecMetricNames) {
        this.ssmClient = ssmClient;
        this.pnPecMetricNames = pnPecMetricNames;
        this.metricsDimensionParser = new MetricsDimensionParser();
    }

    /**
     * Init method to initialize the metrics dimensions schema. It gets the schema from the SSM parameter store.
     */
    @PostConstruct
    public void init() {
        String jsonSchema = ssmClient.getParameter(builder -> builder.name(pnPecDimensionsSchemaName)).parameter().value();
        dimensionsSchema = metricsDimensionParser.parsePecDimensionJson(jsonSchema);
    }

    /**
     * Gets the dimension for a given signature type and file valueInRange.
     *
     * @param valueInRange  the value in range
     * @param dimensionName the dimension name
     * @return the dimension
     */
    public Dimension getDimension(String dimensionName, Long valueInRange) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GET_DIMENSION, valueInRange);
        //TODO to implement
        return null;
    }

}
