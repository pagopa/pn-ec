package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.exception.ses.SesEventException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.SesEventsUtils;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.email.model.dto.ses.SesBounceDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesComplaintDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesDeliveryDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesEmailDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesNotificationDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesRejectDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.stream.Stream;

import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class LavorazioneSesEventsServiceTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    private PnEcConfig.Email.SqsQueue emailSqsQueueName() {
        return pnEcConfig.getEmail().getSqsQueue();
    }

    @MockitoSpyBean
    private LavorazioneSesEventsService service;

    @MockitoSpyBean
    private SqsService sqsService;

    @MockitoBean
    private GestoreRepositoryCall gestoreRepositoryCall;

    @MockitoBean
    private Acknowledgement acknowledgement;


    @ParameterizedTest
    @MethodSource("eventTypeSes")
    void testLavorazioneSesEventsWithValidEvent(String eventType, String messageId, String requestId) {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(eventType);
        SesEmailDto mail = new SesEmailDto();
        mail.setMessageId(messageId);
        dto.setMail(mail);
        if (SesEventsUtils.BOUNCE_EVENT.equalsIgnoreCase(eventType)) {
            SesBounceDto bounce = new SesBounceDto();
            bounce.setBounceType("Permanent");
            dto.setBounce(bounce);
        }
        RequestDto request = new RequestDto();
        request.setRequestIdx(requestId);
        request.setxPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE);

        when(gestoreRepositoryCall.getRequestMetadataByMessageId(messageId)).thenReturn(Mono.just(request));
        when(sqsService.send(any(), any(NotificationTrackerQueueDto.class)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        StepVerifier.create(service.lavorazioneSesEvents(dto, "queue", acknowledgement))
                .expectNextCount(1)
                .verifyComplete();

        verify(sqsService).send(any(), any(NotificationTrackerQueueDto.class));
    }

    @Test
    void testLavorazioneSesEventsWithMessageIdNull() {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(SesEventsUtils.COMPLAINT_EVENT);
        dto.setMail(new SesEmailDto()); // messageId null

        StepVerifier.create(service.lavorazioneSesEvents(dto, "queue", acknowledgement))
                .expectError(SesEventException.MessageIdNullOrEmpty.class)
                .verify();
    }

    @Test
    void testLavorazioneSesEventsWithEventTypeNull() {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setMail(new SesEmailDto());
        dto.getMail().setMessageId("msg-123");
        dto.setNotificationType(null);

        StepVerifier.create(service.lavorazioneSesEvents(dto, "queue", acknowledgement))
                .expectError(SesEventException.EventTypeNullOrEmpty.class)
                .verify();
    }

    @Test
    void testLavorazioneSesEventsWithRequestNotFound() {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(SesEventsUtils.DELIVERY_EVENT);
        SesEmailDto mail = new SesEmailDto();
        mail.setMessageId("msg-123");
        dto.setMail(mail);

        when(gestoreRepositoryCall.getRequestMetadataByMessageId("msg-123")).thenReturn(Mono.empty());

        StepVerifier.create(service.lavorazioneSesEvents(dto, "queue", acknowledgement))
                .expectError(RepositoryManagerException.RequestByMessageIdNotFoundException.class)
                .verify();
    }

    @ParameterizedTest
    @MethodSource("bounceTypes")
    void testLavorazioneSesEventsBounceAllTypes(String bounceType, String bounceSubType, boolean withBounceObject, String expectedEventDetails) {
        String messageId = "msg-bounce-" + bounceType + "-" + bounceSubType;
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(SesEventsUtils.BOUNCE_EVENT);

        SesEmailDto mail = new SesEmailDto();
        mail.setMessageId(messageId);
        dto.setMail(mail);

        if (withBounceObject) {
            SesBounceDto bounce = new SesBounceDto();
            bounce.setBounceType(bounceType);
            bounce.setBounceSubType(bounceSubType);
            dto.setBounce(bounce);
        }

        RequestDto request = new RequestDto();
        request.setRequestIdx("requestId-bounce");
        request.setxPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE);

        when(gestoreRepositoryCall.getRequestMetadataByMessageId(messageId)).thenReturn(Mono.just(request));
        when(sqsService.send(any(), any(NotificationTrackerQueueDto.class)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        String queueName = emailSqsQueueName().getSesEventsName();
        StepVerifier.create(service.lavorazioneSesEvents(dto, queueName, acknowledgement))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<NotificationTrackerQueueDto> captor = ArgumentCaptor.forClass(NotificationTrackerQueueDto.class);
        verify(sqsService, times(1)).send(eq(pnEcConfig.getNotificationTracker().getSqsQueue().getStatoEmailName()), captor.capture());
        assertEquals(SesEventsUtils.BOUNCED, captor.getValue().getNextStatus());
        assertEquals(expectedEventDetails, captor.getValue().getDigitalProgressStatusDto().getEventDetails());
    }

    static Stream<Arguments> bounceTypes() {
        return Stream.of(
                Arguments.of("Permanent", "General", true, "Permanent - General"),
                Arguments.of("Transient", "MailboxFull", true, "Transient - MailboxFull"),
                Arguments.of("Undetermined", "Undetermined", true, "Undetermined - Undetermined"),
                Arguments.of("Permanent", null, true, "Permanent"),
                Arguments.of(null, null, true, null),
                Arguments.of("NoBounceObject", null, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("eventDetailsByEventType")
    void testLavorazioneSesEventsEventDetails(SesNotificationDto dto, String expectedStatus, String expectedEventDetails) {
        String messageId = dto.getMail().getMessageId();

        RequestDto request = new RequestDto();
        request.setRequestIdx("requestId-" + messageId);
        request.setxPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE);

        when(gestoreRepositoryCall.getRequestMetadataByMessageId(messageId)).thenReturn(Mono.just(request));
        when(sqsService.send(any(), any(NotificationTrackerQueueDto.class)))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        String queueName = emailSqsQueueName().getSesEventsName();
        StepVerifier.create(service.lavorazioneSesEvents(dto, queueName, acknowledgement))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<NotificationTrackerQueueDto> captor = ArgumentCaptor.forClass(NotificationTrackerQueueDto.class);
        verify(sqsService, times(1)).send(eq(pnEcConfig.getNotificationTracker().getSqsQueue().getStatoEmailName()), captor.capture());
        assertEquals(expectedStatus, captor.getValue().getNextStatus());
        assertEquals(expectedEventDetails, captor.getValue().getDigitalProgressStatusDto().getEventDetails());
    }

    static Stream<Arguments> eventDetailsByEventType() {
        SesNotificationDto delivery = sesNotification(SesEventsUtils.DELIVERY_EVENT, "msg-delivery");
        SesDeliveryDto deliveryDto = new SesDeliveryDto();
        deliveryDto.setReportingMTA("a8-70.smtp-out.amazonses.com");
        deliveryDto.setSmtpResponse("250 2.0.0 OK");
        delivery.setDelivery(deliveryDto);

        SesNotificationDto deliveryWithoutObject = sesNotification(SesEventsUtils.DELIVERY_EVENT, "msg-delivery-empty");

        SesNotificationDto complaint = sesNotification(SesEventsUtils.COMPLAINT_EVENT, "msg-complaint");
        SesComplaintDto complaintDto = new SesComplaintDto();
        complaintDto.setComplaintFeedbackType("abuse");
        complaint.setComplaint(complaintDto);

        SesNotificationDto complaintWithoutFeedback = sesNotification(SesEventsUtils.COMPLAINT_EVENT, "msg-complaint-empty");
        complaintWithoutFeedback.setComplaint(new SesComplaintDto());

        SesNotificationDto reject = sesNotification(SesEventsUtils.REJECT_EVENT, "msg-reject");
        SesRejectDto rejectDto = new SesRejectDto();
        rejectDto.setReason("Bad content");
        reject.setReject(rejectDto);

        return Stream.of(
                Arguments.of(delivery, SesEventsUtils.DELIVERED, "a8-70.smtp-out.amazonses.com - 250 2.0.0 OK"),
                Arguments.of(deliveryWithoutObject, SesEventsUtils.DELIVERED, null),
                Arguments.of(complaint, SesEventsUtils.COMPLAINT, "abuse"),
                Arguments.of(complaintWithoutFeedback, SesEventsUtils.COMPLAINT, null),
                Arguments.of(reject, SesEventsUtils.REJECTED, "Bad content")
        );
    }

    private static SesNotificationDto sesNotification(String eventType, String messageId) {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(eventType);
        SesEmailDto mail = new SesEmailDto();
        mail.setMessageId(messageId);
        dto.setMail(mail);
        return dto;
    }

    static Stream<Arguments> eventTypeSes() {
        return Stream.of(
                Arguments.of(SesEventsUtils.DELIVERY_EVENT, "messageId-delivery", "requestId-delivery"),
                Arguments.of(SesEventsUtils.BOUNCE_EVENT, "messageId-bounce", "requestId-bounce"),
                Arguments.of(SesEventsUtils.COMPLAINT_EVENT, "messageId-complaint", "requestId-complaint"),
                Arguments.of(SesEventsUtils.REJECT_EVENT, "messageId-reject", "requestId-reject")
        );
    }

}
