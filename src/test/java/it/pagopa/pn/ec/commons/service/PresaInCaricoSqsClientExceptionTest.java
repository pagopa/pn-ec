package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.email.service.EmailService;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.PRESA_IN_CARICO_PEC;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.TEXT_PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PresaInCaricoSqsClientExceptionTest {

    private static final String REQUEST_IDX = "requestIdxReachability";

    @Autowired
    private PnEcConfig pnEcConfig;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private GestoreRepositoryCall gestoreRepositoryCall;

    @MockitoBean
    private AttachmentServiceImpl attachmentService;

    @MockitoSpyBean
    private SqsServiceImpl sqsService;

    @MockitoSpyBean
    private EmailService emailService;

    @MockitoSpyBean
    private SmsService smsService;

    @MockitoSpyBean
    private PecService pecService;

    @BeforeEach
    void setUp() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(new ClientConfigurationInternalDto()));
        when(gestoreRepositoryCall.insertRichiesta(any())).thenReturn(Mono.just(new RequestDto()));
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), anyString(), anyBoolean())).thenReturn(Flux.just(new FileDownloadResponse()));
    }

    private static EmailPresaInCaricoInfo emailInfo() {
        return EmailPresaInCaricoInfo.builder()
                .requestIdx(REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesyMailRequest(createMailRequest(0))
                .build();
    }

    private static SmsPresaInCaricoInfo smsInfo() {
        return SmsPresaInCaricoInfo.builder()
                .requestIdx(REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesySmsRequest(createSmsRequest())
                .build();
    }

    private static PecPresaInCaricoInfo pecInfo() {
        DigitalNotificationRequest request = new DigitalNotificationRequest();
        request.setRequestId(REQUEST_IDX);
        request.eventType("string");
        request.setClientRequestTimeStamp(OffsetDateTime.now());
        request.setQos(INTERACTIVE);
        request.setReceiverDigitalAddress("receiver@pec.it");
        request.setMessageText("string");
        request.channel(PEC);
        request.setSubjectText("subject");
        request.setMessageContentType(TEXT_PLAIN);
        request.setAttachmentUrls(List.of("safestorage://prova.pdf"));
        return PecPresaInCaricoInfo.builder()
                .requestIdx(REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalNotificationRequest(request)
                .build();
    }

    private void failEveryPublish() {
        doReturn(Mono.error(new SqsClientException("queue"))).when(sqsService).send(anyString(), any());
    }

    private void failOnlyPublishOn(String queueName) {
        doReturn(Mono.just(SendMessageResponse.builder().build())).when(sqsService).send(anyString(), any());
        doReturn(Mono.error(new SqsClientException(queueName))).when(sqsService).send(eq(queueName), any());
    }

    private static void assertRetryExhaustedOverSqsClientException(Throwable error) {
        assertTrue(Exceptions.isRetryExhausted(error));
        assertInstanceOf(SqsClientException.class, error.getCause());
    }

    @Test
    void email_sqsClientExceptionOnEveryPublish_isRetryExhaustedAndInternalErrorIsNotPublished() {
        EmailPresaInCaricoInfo info = emailInfo();
        failEveryPublish();

        StepVerifier.create(emailService.presaInCarico(info)).expectErrorSatisfies(PresaInCaricoSqsClientExceptionTest::assertRetryExhaustedOverSqsClientException).verify();

        verify(emailService, never()).sendNotificationOnStatusQueue(eq(info), eq(Status.INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void email_sqsClientExceptionOnInteractiveQueue_isRetryExhaustedAndInternalErrorIsNotPublished() {
        EmailPresaInCaricoInfo info = emailInfo();
        failOnlyPublishOn(pnEcConfig.getEmail().getSqsQueue().getInteractiveName());

        StepVerifier.create(emailService.presaInCarico(info)).expectErrorSatisfies(PresaInCaricoSqsClientExceptionTest::assertRetryExhaustedOverSqsClientException).verify();

        verify(emailService, never()).sendNotificationOnStatusQueue(eq(info), eq(Status.INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void sms_sqsClientExceptionOnEveryPublish_isRetryExhaustedAndInternalErrorIsNotPublished() {
        SmsPresaInCaricoInfo info = smsInfo();
        failEveryPublish();

        StepVerifier.create(smsService.presaInCarico(info)).expectErrorSatisfies(PresaInCaricoSqsClientExceptionTest::assertRetryExhaustedOverSqsClientException).verify();

        verify(smsService, never()).sendNotificationOnStatusQueue(eq(info), eq(Status.INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void sms_sqsClientExceptionOnInteractiveQueue_isRetryExhaustedAndInternalErrorIsNotPublished() {
        SmsPresaInCaricoInfo info = smsInfo();
        failOnlyPublishOn(pnEcConfig.getSms().getSqsQueue().getInteractiveName());

        StepVerifier.create(smsService.presaInCarico(info)).expectErrorSatisfies(PresaInCaricoSqsClientExceptionTest::assertRetryExhaustedOverSqsClientException).verify();

        verify(smsService, never()).sendNotificationOnStatusQueue(eq(info), eq(Status.INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void pec_sqsClientExceptionOnBookedPublish_isRetryExhaustedAndInternalErrorIsNotPublished() {
        PecPresaInCaricoInfo info = pecInfo();
        failEveryPublish();

        StepVerifier.create(pecService.presaInCarico(info)).expectErrorSatisfies(PresaInCaricoSqsClientExceptionTest::assertRetryExhaustedOverSqsClientException).verify();

        verify(pecService, never()).sendNotificationOnStatusQueue(eq(info), eq(Status.INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void pec_sqsClientExceptionOnInteractiveQueue_isPropagatedAndInternalErrorIsPublishedWithErrorDetails() {
        PecPresaInCaricoInfo info = pecInfo();
        failOnlyPublishOn(pnEcConfig.getPec().getSqsQueue().getInteractiveName());

        StepVerifier.create(pecService.presaInCarico(info)).expectError(SqsClientException.class).verify();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(Status.INTERNAL_ERROR.getStatusTransactionTableCompliant()), captor.capture());
        String errorDetails = captor.getValue().getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(PRESA_IN_CARICO_PEC));
        assertTrue(errorDetails.contains("An error occurred during client operation"));
        assertFalse(errorDetails.contains("\tat "));
    }

}
