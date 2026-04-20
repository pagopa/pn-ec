package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.exception.ses.SesEventException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.SesEventsUtils;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.dto.ses.SesBounceDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesEmailDto;
import it.pagopa.pn.ec.email.model.dto.ses.SesNotificationDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sqs.SqsTimeoutProvider;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.ec.util.LogSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
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
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class LavorazioneSesEventsServiceTest {

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

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
            bounce.setBounceType(SesEventsUtils.BOUNCE_TYPE_PERMANENT);
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

    @Test
    void testLavorazioneSesEventsWithNonPermanentBounce() {
        SesNotificationDto dto = new SesNotificationDto();
        dto.setNotificationType(SesEventsUtils.BOUNCE_EVENT);

        SesEmailDto mail = new SesEmailDto();
        mail.setMessageId("msg-123");
        dto.setMail(mail);

        SesBounceDto bounce = new SesBounceDto();
        bounce.setBounceType("Transient");
        dto.setBounce(bounce);
        String queueName = emailSqsQueueName.sesEventsName();
        StepVerifier.create(service.lavorazioneSesEvents(dto, queueName, acknowledgement)).expectComplete().verify();
        verifyNoInteractions(sqsService);
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
