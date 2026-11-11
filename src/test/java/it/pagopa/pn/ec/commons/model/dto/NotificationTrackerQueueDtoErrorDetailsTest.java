package it.pagopa.pn.ec.commons.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationTrackerQueueDtoErrorDetailsTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private final EmailPresaInCaricoInfo presaInCaricoInfo = EmailPresaInCaricoInfo.builder().requestIdx("requestIdx").xPagopaExtchCxId("clientId").build();

    @Test
    void digitalFactory_keepsErrorDetails() {
        DigitalProgressStatusDto dto = new DigitalProgressStatusDto().errorDetails("failure");

        NotificationTrackerQueueDto queueDto = createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, "retry", dto);

        assertEquals("failure", queueDto.getDigitalProgressStatusDto().getErrorDetails());
    }

    @Test
    void paperFactory_keepsErrorDetails() {
        PaperProgressStatusDto dto = new PaperProgressStatusDto().errorDetails("failure");

        NotificationTrackerQueueDto queueDto = createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, "retry", dto);

        assertEquals("failure", queueDto.getPaperProgressStatusDto().getErrorDetails());
    }

    @Test
    void digitalQueueMessage_serializationRoundTrip_keepsErrorDetails() throws Exception {
        DigitalProgressStatusDto dto = new DigitalProgressStatusDto().errorDetails("failure");
        NotificationTrackerQueueDto queueDto = createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, "retry", dto);

        String json = objectMapper.writeValueAsString(queueDto);
        NotificationTrackerQueueDto back = objectMapper.readValue(json, NotificationTrackerQueueDto.class);

        assertEquals("failure", back.getDigitalProgressStatusDto().getErrorDetails());
    }

    @Test
    void paperQueueMessage_serializationRoundTrip_keepsErrorDetails() throws Exception {
        PaperProgressStatusDto dto = new PaperProgressStatusDto().errorDetails("failure");
        NotificationTrackerQueueDto queueDto = createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, "retry", dto);

        String json = objectMapper.writeValueAsString(queueDto);
        NotificationTrackerQueueDto back = objectMapper.readValue(json, NotificationTrackerQueueDto.class);

        assertEquals("failure", back.getPaperProgressStatusDto().getErrorDetails());
    }

}
