package it.pagopa.pn.ec.cartaceo.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.StatusCodesToDeliveryFailureCauses;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import it.pagopa.pn.ec.commons.utils.LogUtils;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.CLIENT_METHOD_INVOCATION_WITH_ARGS;

@CustomLog
@Configuration
public class StatusCodesToDeliveryFailureCausesConf {

    private final SsmClient ssmClient;
    @Value("${pn.ec.esiti-cartaceo.parameter.name}")
    private String DELIVERY_FAILURE_CODES_PARAMETER_NAME;

    @Value("${DefaultEsitiCartaceoParameterValue:#{null}}")
    private String DEFAULT_MAP_VALUE;


    ObjectMapper objectMapper = new ObjectMapper();
    JsonUtils jsonUtils = new JsonUtils(objectMapper);

    public StatusCodesToDeliveryFailureCausesConf(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

    public String getParameter(String parameterName) throws SsmException {
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS,"ssmClient.getParameter", parameterName);
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterName).build();

        return ssmClient.getParameter(parameterRequest).parameter().value();
    }

    public Map<String, Map<String,List<String>>> retrieveDeliveryFailureCausesFromParameterStore() throws SsmException, JsonProcessingException {
        String parameterName = DELIVERY_FAILURE_CODES_PARAMETER_NAME;
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterName).build();
        String value = DEFAULT_MAP_VALUE;
        if (DEFAULT_MAP_VALUE == null) {
            parameterRequest = parameterRequest.toBuilder().build();
            log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS,"ssmClient.getParameter", parameterName);
            value = ssmClient.getParameter(parameterRequest).parameter().value();
        }
        JsonNode jsonNode;
        jsonNode = objectMapper.readTree(value);
        String cartaceoJson = jsonNode.get("cartaceo").toString();

        return jsonUtils.convertJsonToMap(cartaceoJson);
    }

    @SneakyThrows
    @Bean
    public StatusCodesToDeliveryFailureCauses statusCodesToDeliveryFailureCauses(){
            return new StatusCodesToDeliveryFailureCauses(retrieveDeliveryFailureCausesFromParameterStore());
    }
}
