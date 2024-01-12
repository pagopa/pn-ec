package it.pagopa.pn.ec.cartaceo.model.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.ec.consolidatore.utils.PaperElem;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTestWebEnv
@CustomLog
public class StatusCodesToDeliveryFailureCausesTest {

    StatusCodesToDeliveryFailureCauses statusCodesToCustomDeliveryFailureCauses;
    private List<String> statusCodesListFromPaperElem = PaperElem.statusCodeDescriptionMap().keySet().stream().toList();
    private List<String> deliveryFailureCausesListFromPaperElem = PaperElem.deliveryFailureCausemap().keySet().stream().toList();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        statusCodesToCustomDeliveryFailureCauses = new StatusCodesToDeliveryFailureCauses();

    }

    @Test
    public void retrieveDeliveryFailureCauses_whenCalled_returnsDeliveryFailureCausesMapTest() throws JsonProcessingException {

        statusCodesToCustomDeliveryFailureCauses.setStatusCodeToDeliveryFailureCausesMap(getCustomDeliveryFailureCausesListFromPaperElem());

        assertNotNull(statusCodesToCustomDeliveryFailureCauses);

        assertEquals(3, statusCodesToCustomDeliveryFailureCauses.getStatusCodeToDeliveryFailureCausesMap().size());
        assertEquals(2, statusCodesToCustomDeliveryFailureCauses.getStatusCodeToDeliveryFailureCausesMap().get(statusCodesListFromPaperElem.get(0)).get("deliveryFailureCause").size());
        assertEquals(3, statusCodesToCustomDeliveryFailureCauses.getStatusCodeToDeliveryFailureCausesMap().get(statusCodesListFromPaperElem.get(1)).get("deliveryFailureCause").size());
        assertEquals(4, statusCodesToCustomDeliveryFailureCauses.getStatusCodeToDeliveryFailureCausesMap().get(statusCodesListFromPaperElem.get(2)).get("deliveryFailureCause").size());
    }

    @Test
    public void shouldReturnTrueWhenDeliveryFailureCauseExistsInStatusCodeTest() throws JsonProcessingException {

        statusCodesToCustomDeliveryFailureCauses.setStatusCodeToDeliveryFailureCausesMap(getCustomDeliveryFailureCauses());

        assertNotNull(statusCodesToCustomDeliveryFailureCauses);
        assertTrue(statusCodesToCustomDeliveryFailureCauses.isDeliveryFailureCauseInStatusCode("code1", "notification1"));
        assertFalse(statusCodesToCustomDeliveryFailureCauses.isDeliveryFailureCauseInStatusCode("code1", "notification3"));
    }

    private Map<String, Map<String, List<String>>> getCustomDeliveryFailureCauses() {
        Map<String, Map<String, List<String>>> codes = new HashMap<>();
        codes.put("code1", Map.of("deliveryFailureCause", Arrays.asList("notification1", "notification2")));
        codes.put("code2", Map.of("deliveryFailureCause", Arrays.asList("notification3", "notification4")));
        codes.put("code3", Map.of("deliveryFailureCause", Arrays.asList("notification5", "notification6")));
        return codes;
    }

    private Map<String, Map<String, List<String>>> getCustomDeliveryFailureCausesListFromPaperElem() {
        Map<String, Map<String, List<String>>> codes = new HashMap<>();

        codes.put(statusCodesListFromPaperElem.get(0), Map.of("deliveryFailureCause", deliveryFailureCausesListFromPaperElem.subList(0, 2)));
        codes.put(statusCodesListFromPaperElem.get(1), Map.of("deliveryFailureCause", deliveryFailureCausesListFromPaperElem.subList(2, 5)));
        codes.put(statusCodesListFromPaperElem.get(2), Map.of("deliveryFailureCause", deliveryFailureCausesListFromPaperElem.subList(5, 9)));
        return codes;
    }
}