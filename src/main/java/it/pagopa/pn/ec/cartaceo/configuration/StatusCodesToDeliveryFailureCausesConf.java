package it.pagopa.pn.ec.cartaceo.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.StatusCodesToDeliveryFailureCauses;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

import java.util.List;
import java.util.Map;

@CustomLog
@Configuration
public class StatusCodesToDeliveryFailureCausesConf {

    private final SsmClient ssmClient;
    private static final String URL_PREFIX = "/PagoPA/";
    private static final String DELIVERY_FAILURE_CODES_SUFFIX = "esitiCartaceo";

    ObjectMapper objectMapper = new ObjectMapper();
    JsonUtils jsonUtils = new JsonUtils(objectMapper);

    public StatusCodesToDeliveryFailureCausesConf(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

    public String getParameter(String parameterName) throws SsmException {
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterName).build();

        return ssmClient.getParameter(parameterRequest).parameter().value();
    }

    public Map<String, List<String>> retrieveDeliveryFailureCausesFromParameterStore() throws SsmException, JsonProcessingException {
        String parameterName = URL_PREFIX + DELIVERY_FAILURE_CODES_SUFFIX;
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterName).build();
        String value = ssmClient.getParameter(parameterRequest).parameter().value();

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
