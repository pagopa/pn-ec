package it.pagopa.pn.ec.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.email.model.dto.ses.*;
import it.pagopa.pn.ec.util.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.utils.SesEventsUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SesEventsUtilsTest {

    private final LogSanitizer logSanitizer = new LogSanitizer();

    @ParameterizedTest
    @MethodSource("eventDetailsCases")
    void testBuildEventDetails(SesNotificationDto dto, String expected) {
        assertEquals(expected, buildEventDetails(dto, logSanitizer));
    }

    @Test
    void testBuildEventDetailsWithEventTypeNull() {
        assertNull(buildEventDetails(new SesNotificationDto(), logSanitizer));
    }

    @Test
    void testDeserializeComplaintFeedbackType() throws JsonProcessingException {
        String json = """
                {
                  "eventType": "Complaint",
                  "mail": { "messageId": "msg-complaint" },
                  "complaint": {
                    "complainedRecipients": [ { "emailAddress": "recipient@example.com" } ],
                    "timestamp": "2026-11-05T10:00:00.000Z",
                    "feedbackId": "feedback-id",
                    "complaintFeedbackType": "abuse"
                  }
                }
                """;
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SesNotificationDto dto = objectMapper.readValue(json, SesNotificationDto.class);

        assertEquals("abuse", dto.getComplaint().getComplaintFeedbackType());
        assertEquals("abuse", buildEventDetails(dto, logSanitizer));
    }

    static Stream<Arguments> eventDetailsCases() {
        return Stream.of(
                // Bounce
                Arguments.of(bounce("Transient", "MailboxFull"), "Transient - MailboxFull"),
                Arguments.of(bounce("Permanent", null), "Permanent"),
                Arguments.of(bounce(null, "General"), "General"),
                Arguments.of(bounce(" ", ""), null),
                Arguments.of(notification(BOUNCE_EVENT), null),
                // Delivery
                Arguments.of(delivery("a8-70.smtp-out.amazonses.com", "250 2.0.0 OK"), "a8-70.smtp-out.amazonses.com - 250 2.0.0 OK"),
                Arguments.of(delivery("mta.example.com", "250 Ok <mario.rossi@example.com> queued"), "mta.example.com - 250 Ok <xxx@example.com> queued"),
                Arguments.of(delivery("mx.google.com", "250 2.0.0 OK  1730800000 d2e1a72fcca58-7f3b1c2si"), "mx.google.com - 250 2.0.0 OK  1730800000 d2e1a72fcca58-7f3b1c2si"),
                Arguments.of(delivery(null, "250 2.0.0 OK"), "250 2.0.0 OK"),
                Arguments.of(delivery("", null), null),
                Arguments.of(notification(DELIVERY_EVENT), null),
                // Reject
                Arguments.of(reject("Bad content"), "Bad content"),
                Arguments.of(notification(REJECT_EVENT), null),
                // Complaint
                Arguments.of(complaint("abuse"), "abuse"),
                Arguments.of(complaint(null), null),
                Arguments.of(notification(COMPLAINT_EVENT), null)
        );
    }

    private static SesNotificationDto notification(String eventType) {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(eventType);
        return dto;
    }

    private static SesNotificationDto bounce(String bounceType, String bounceSubType) {
        SesNotificationDto dto = notification(BOUNCE_EVENT);
        SesBounceDto bounce = new SesBounceDto();
        bounce.setBounceType(bounceType);
        bounce.setBounceSubType(bounceSubType);
        dto.setBounce(bounce);
        return dto;
    }

    private static SesNotificationDto delivery(String reportingMTA, String smtpResponse) {
        SesNotificationDto dto = notification(DELIVERY_EVENT);
        SesDeliveryDto delivery = new SesDeliveryDto();
        delivery.setReportingMTA(reportingMTA);
        delivery.setSmtpResponse(smtpResponse);
        dto.setDelivery(delivery);
        return dto;
    }

    private static SesNotificationDto reject(String reason) {
        SesNotificationDto dto = notification(REJECT_EVENT);
        SesRejectDto reject = new SesRejectDto();
        reject.setReason(reason);
        dto.setReject(reject);
        return dto;
    }

    private static SesNotificationDto complaint(String complaintFeedbackType) {
        SesNotificationDto dto = notification(COMPLAINT_EVENT);
        SesComplaintDto complaint = new SesComplaintDto();
        complaint.setComplaintFeedbackType(complaintFeedbackType);
        dto.setComplaint(complaint);
        return dto;
    }
}
