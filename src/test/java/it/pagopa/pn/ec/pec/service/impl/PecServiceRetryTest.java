package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@SpringBootTestWebEnv
class PecServiceRetryTest {



    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private PecSqsQueueName pecSqsQueueName;

    @MockBean
    private FileCall uriBuilderCall;

    @SpyBean
    GestoreRepositoryCall gestoreRepositoryCall;

    @MockBean
    private AuthService authService;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
   private ArubaCall arubaCall;

    @Autowired
     PecService pecService;

    @Mock
    private Acknowledgment acknowledgment;

    Message message = Message.builder().build();
    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final RequestDto requestDto = new RequestDto();
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";
    public static DigitalNotificationRequest createDigitalNotificationRequest() {
//        Mock an existing request. Set the requestIdx
        requestDto.setRequestIdx("requestIdx");

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(defaultAttachmentUrl);

        digitalNotificationRequest.setRequestId("requestIdx");
        digitalNotificationRequest.eventType("string");
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress("pippo@pec.it");
        digitalNotificationRequest.setMessageText("string");
        digitalNotificationRequest.channel(PEC);
        digitalNotificationRequest.setSubjectText("prova testo");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentsUrls(defaultListAttachmentUrls);
        return digitalNotificationRequest;
    }
    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO = PecPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createDigitalNotificationRequest())
            .build();

    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO_TEST = PecPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(new DigitalNotificationRequest())
            .build();


    @Test
    void testGestioneRetryPecScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(eq(pecSqsQueueName.errorName()), eq(PecPresaInCaricoInfo.class)))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        pecService.gestioneRetryPecScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }


    @Test
    void gestionreRetryPec_GenericError(){

        String requestId = "idTest";
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        String clientId = DEFAULT_ID_CLIENT_HEADER_VALUE;
        requestDto.setxPagopaExtchCxId(clientId);
        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestDto.setRequestMetadata(requestMetadata);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));
        //when(gestoreRepositoryCall.patchRichiesta(eq(requestId), eq(patchDto)).thenReturn(Mono.just(requestDto)));

        DeleteMessageResponse response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_TEST, message).block();

        Assert.assertNull(response);
    }

    @Test
    void gestionreRetryPec_GenericErrorTest(){

        String requestId = "idTest";
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        String clientId = DEFAULT_ID_CLIENT_HEADER_VALUE;
        requestDto.setxPagopaExtchCxId(clientId);
        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestDto.setRequestMetadata(requestMetadata);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));
        //when(gestoreRepositoryCall.patchRichiesta(eq(requestId), eq(patchDto)).thenReturn(Mono.just(requestDto)));

        DeleteMessageResponse response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO, message).block();

        Assert.assertNull(response);
    }

    @Test
    void gestionreRetryPec_Retry_Ok(){

        String requestId = "idTest";
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        String clientId = DEFAULT_ID_CLIENT_HEADER_VALUE;
        requestDto.setxPagopaExtchCxId(clientId);


        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestMetadata.getRetry().setRetryStep(BigDecimal.valueOf(0));
        requestDto.setRequestMetadata(requestMetadata);
        requestDto.getRequestMetadata().getRetry().setRetryPolicy(retries);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));


        DeleteMessageResponse response =  pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO, message).block();

        Assert.assertNull(response);
    }


    @Test
    void gestionreLavorazionePec_Ok(){

        String requestId = "idTest";
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        String clientId = DEFAULT_ID_CLIENT_HEADER_VALUE;
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();



        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestMetadata.getRetry().setRetryStep(BigDecimal.valueOf(0));
        requestDto.setRequestMetadata(requestMetadata);
        requestDto.getRequestMetadata().getRetry().setRetryPolicy(retries);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));


          pecService.lavorazioneRichiestaInteractive(PEC_PRESA_IN_CARICO_INFO, acknowledgment);

//        Assert.assertNull(response);
    }


    @Test
    void testGestioneRetryPecSchedulerBach_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(eq(pecSqsQueueName.batchName()), eq(PecPresaInCaricoInfo.class)))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        pecService.lavorazioneRichiestaBatch();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }

}