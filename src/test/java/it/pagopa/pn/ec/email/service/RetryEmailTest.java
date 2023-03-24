package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.email.service.EmailService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTestWebEnv
public class RetryEmailTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SesService sesService;

    @Mock
    private Acknowledgment acknowledgment;
    @SpyBean
    GestoreRepositoryCall gestoreRepositoryCall;
    @SpyBean
    AttachmentServiceImpl attachmentService;

    EmailPresaInCaricoInfo emailPresaInCaricoInfo = new EmailPresaInCaricoInfo();

    Message message = Message.builder().build();
    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH = EmailPresaInCaricoInfo.builder()
            .requestIdx(
                    DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(
                    createMailRequest(1))
            .build();



    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO = EmailPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(createMailRequest(0))
            .build();


    private final SqsMessageWrapper<EmailPresaInCaricoInfo> sqsPresaInCaricoInfo = new SqsMessageWrapper<>(message, EMAIL_PRESA_IN_CARICO_INFO);

    @Test
    void testGestioneRetryEmailScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(eq(emailSqsQueueName.errorName()), eq(EmailPresaInCaricoInfo.class)))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        emailService.gestioneRetryEmailScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }

    @Test
    void gestionreRetryEmail_GenericError(){

        String requestId = "idTest";
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestDto.setRequestMetadata(requestMetadata);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(requestId))).thenReturn(Mono.just(requestDto));
        //when(gestoreRepositoryCall.patchRichiesta(eq(requestId), eq(patchDto)).thenReturn(Mono.just(requestDto)));

        DeleteMessageResponse response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message).block();

        Assert.assertNull(response);
    }

    @Test
    void gestionreRetryEmail_Retry_Ok(){

        String requestId = "idTest";
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);


        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestMetadata.getRetry().setRetryStep(BigDecimal.valueOf(0));
        requestDto.setRequestMetadata(requestMetadata);
        requestDto.getRequestMetadata().getRetry().setRetryPolicy(retries);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(requestId))).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(requestId, patchDto)).thenReturn(Mono.just(requestDto));


        DeleteMessageResponse response =  emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message).block();

        Assert.assertNull(response);
    }


    @Test
    void gestionreRetryEmailAttachment_GenericError(){

        String requestId = "idTest";
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestDto.setRequestMetadata(requestMetadata);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
        DigitalCourtesyMailRequest digitalCourtesyMailRequest = new DigitalCourtesyMailRequest();

        List<String> attachList = new ArrayList<>();
        int attachNum = 1;
        for (int idx = 0; idx < attachNum; idx++) {
            attachList.add("safestorage://PN_EXTERNAL_LEGAL_FACTS-14d277f9beb4c8a9da322092c350d51");
        }
        digitalCourtesyMailRequest.setAttachmentsUrls(attachList);


        when(gestoreRepositoryCall.getRichiesta(eq(requestId))).thenReturn(Mono.just(requestDto));


        DeleteMessageResponse response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message).block();

        Assert.assertNull(response);
    }

    @Test
    void gestionreRetryEmailAttachment_Retry_Ok(){

        String requestId = "idTest";
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);


        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestMetadata.getRetry().setRetryStep(BigDecimal.valueOf(0));
        requestDto.setRequestMetadata(requestMetadata);
        requestDto.getRequestMetadata().getRetry().setRetryPolicy(retries);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(requestId))).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(requestId, patchDto)).thenReturn(Mono.just(requestDto));


        DeleteMessageResponse response =  emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message).block();

        Assert.assertNull(response);
    }


    @Test
    void testGestioneRetryEmailSchedulerBach_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(eq(emailSqsQueueName.batchName()), eq(EmailPresaInCaricoInfo.class)))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        emailService.lavorazioneRichiestaBatch();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }
}
