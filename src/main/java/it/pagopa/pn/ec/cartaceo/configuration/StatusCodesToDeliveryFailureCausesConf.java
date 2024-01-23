package it.pagopa.pn.ec.cartaceo.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.StatusCodesToDeliveryFailureCauses;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
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
    ObjectMapper objectMapper = new ObjectMapper();
    JsonUtils jsonUtils = new JsonUtils(objectMapper);

    public StatusCodesToDeliveryFailureCausesConf(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

    public Map<String, Map<String,List<String>>> retrieveDeliveryFailureCausesFromParameterStore() throws SsmException, JsonProcessingException {
        String parameterName = DELIVERY_FAILURE_CODES_PARAMETER_NAME;
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterName).build();
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS,"ssmClient.getParameter", parameterName);
        String  value = ssmClient.getParameter(parameterRequest).parameter().value();
        JsonNode jsonNode = objectMapper.readTree(value);
        String cartaceoJson = jsonNode.get("cartaceo").toString();

        return jsonUtils.convertJsonToMap(cartaceoJson);
    }

    @SneakyThrows
    @Bean
    public StatusCodesToDeliveryFailureCauses statusCodesToDeliveryFailureCauses(){
            return new StatusCodesToDeliveryFailureCauses(retrieveDeliveryFailureCausesFromParameterStore());
    }
}
