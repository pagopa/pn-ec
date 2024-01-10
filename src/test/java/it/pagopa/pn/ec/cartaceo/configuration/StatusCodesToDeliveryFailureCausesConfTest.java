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
    private static final String PARAMETER = "/PagoPA/esitiCartaceo";
    private static final String EXPECTED_VALUE = "{\n" +
            "\"cartaceo\":{\n" +
            "     \"RECRN006\" : [\"M03\",\"M04\"],\n" +
            "     \"RECRN004A\" : [\"M05\",\"M06\",\"M07\"],\n" +
            "     \"RECRN004B\" : [\"M08\",\"M09\",\"F01\",\"F02\"]\n" +
            "    }\n" +
            "}";

    @Test
    void getParameterTest() {
        String actualValue = statusCodesToDeliveryFailureCausesConf.getParameter(PARAMETER);

        assertEquals(EXPECTED_VALUE, actualValue);
    }

    @Test
    void buildDeliveryFailureCausesMapFromJsonTest() throws JsonProcessingException {

        Map<String, List<String>> result = statusCodesToDeliveryFailureCausesConf.retrieveDeliveryFailureCausesFromParameterStore();

        assertEquals(3, result.size());
        assertEquals(List.of("M03", "M04"), result.get("RECRN006"));
        assertEquals(List.of("M05", "M06", "M07"), result.get("RECRN004A"));
        assertEquals(List.of("M08", "M09", "F01", "F02"), result.get("RECRN004B"));
    }
}
