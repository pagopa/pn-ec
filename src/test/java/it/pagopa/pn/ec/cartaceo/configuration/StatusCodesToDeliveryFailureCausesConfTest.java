package it.pagopa.pn.ec.cartaceo.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTestWebEnv
class StatusCodesToDeliveryFailureCausesConfTest {

    @Autowired
    private StatusCodesToDeliveryFailureCausesConf statusCodesToDeliveryFailureCausesConf;

    @Test
    void buildDeliveryFailureCausesMapFromJsonTest() throws JsonProcessingException {

        Map<String, Map<String, List<String>>> result = statusCodesToDeliveryFailureCausesConf.retrieveDeliveryFailureCausesFromParameterStore();

        assertEquals(3, result.size());
        assertEquals(Map.of("deliveryFailureCause", List.of("M03", "M04")), result.get("RECRN006"));
        assertEquals(Map.of("deliveryFailureCause", List.of("M05", "M06", "M07")), result.get("RECRN004A"));
        assertEquals(Map.of("deliveryFailureCause", List.of("M08", "M09", "F01", "F02", "TEST")), result.get("RECRN004B"));
    }
}