package it.pagopa.pn.ec.commons.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTestWebEnv
class ErrorDetailsDeploymentCompatibilityTest {

    @Autowired
    private ObjectMapper objectMapper;

    static class LegacyDigitalProgressStatus {

        public String status;
    }

    static class LegacyQueueMessage {

        public String requestIdx;
        public String nextStatus;
        public LegacyDigitalProgressStatus digitalProgressStatusDto;
    }

    private static final String MESSAGE_WITH_ERROR_DETAILS =
            "{\"requestIdx\":\"requestIdx\",\"nextStatus\":\"retry\",\"digitalProgressStatusDto\":{\"status\":\"retry\",\"errorDetails\":\"failure text\"}}";

    @Test
    void springObjectMapper_messageWithErrorDetailsOnDtoWithoutField_ignoresUnknownProperty() {
        LegacyQueueMessage result = assertDoesNotThrow(() -> objectMapper.readValue(MESSAGE_WITH_ERROR_DETAILS, LegacyQueueMessage.class));

        assertEquals("retry", result.nextStatus);
        assertEquals("retry", result.digitalProgressStatusDto.status);
    }

    @Test
    void springObjectMapper_messageWithUnknownRootAndNestedProperties_isDeserializedIgnoringThem() {
        String json = "{\"requestIdx\":\"requestIdx\",\"nextStatus\":\"retry\",\"digitalProgressStatusDto\":{\"status\":\"retry\",\"unknownFutureField\":\"value\"},\"unknownRootField\":1}";

        NotificationTrackerQueueDto result = assertDoesNotThrow(() -> objectMapper.readValue(json, NotificationTrackerQueueDto.class));

        assertEquals("retry", result.getNextStatus());
        assertEquals("retry", result.getDigitalProgressStatusDto().getStatus());
    }

    @Test
    void springObjectMapper_digitalMessageWithErrorDetails_keepsErrorDetails() throws Exception {
        NotificationTrackerQueueDto result = objectMapper.readValue(MESSAGE_WITH_ERROR_DETAILS, NotificationTrackerQueueDto.class);

        assertEquals("failure text", result.getDigitalProgressStatusDto().getErrorDetails());
    }

    @Test
    void springObjectMapper_paperMessageWithErrorDetails_keepsErrorDetails() throws Exception {
        String json = "{\"requestIdx\":\"requestIdx\",\"nextStatus\":\"retry\",\"paperProgressStatusDto\":{\"status\":\"retry\",\"errorDetails\":\"failure text\"}}";

        NotificationTrackerQueueDto result = objectMapper.readValue(json, NotificationTrackerQueueDto.class);

        assertEquals("failure text", result.getPaperProgressStatusDto().getErrorDetails());
    }

    @Test
    void springObjectMapper_serializedDtosWithErrorDetails_roundTrip() throws Exception {
        DigitalProgressStatusDto digital = new DigitalProgressStatusDto().errorDetails("digital failure");
        PaperProgressStatusDto paper = new PaperProgressStatusDto().errorDetails("paper failure");

        DigitalProgressStatusDto digitalBack = objectMapper.readValue(objectMapper.writeValueAsString(digital), DigitalProgressStatusDto.class);
        PaperProgressStatusDto paperBack = objectMapper.readValue(objectMapper.writeValueAsString(paper), PaperProgressStatusDto.class);

        assertEquals("digital failure", digitalBack.getErrorDetails());
        assertEquals("paper failure", paperBack.getErrorDetails());
    }

}
