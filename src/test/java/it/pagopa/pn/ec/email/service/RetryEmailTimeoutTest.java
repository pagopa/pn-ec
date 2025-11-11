package it.pagopa.pn.ec.email.service;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sqs.SqsTimeoutProvider;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RetryEmailTimeoutTest {

    @SpyBean
    private EmailService emailService;
    @Autowired
    private EmailSqsQueueName emailSqsQueueName;
    @SpyBean
    private SqsServiceImpl sqsService;
    @MockBean
    private FileCall fileCall;
    @SpyBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private SesService sesService;
    @MockBean
    private DownloadCall downloadCall;
    @MockBean
    private SqsTimeoutProvider sqsTimeoutProvider;
    EmailPresaInCaricoInfo emailPresaInCaricoInfo = new EmailPresaInCaricoInfo();
    private static final String QUEUE_NAME = "queue";
    private static final Duration TIMEOUT_INACTIVE_DURATION = Duration.ofSeconds(86400);


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

    private static final FileDownloadResponse FILE_DOWNLOAD_RESPONSE = new FileDownloadResponse()
            .download(new FileDownloadInfo()
                    .url("url"))
            .key("key")
            .checksum("checksum")
            .contentLength(BigDecimal.TEN)
            .contentType("contentType")
            .versionId("versionId")
            .documentStatus("documentStatus")
            .documentType("documentType")
            .retentionUntil(OffsetDateTime.parse("2023-04-18T05:08:27.101Z"));

    private static final StepError STEP_ERROR = StepError.builder()
            .generatedMessageDto(new GeneratedMessageDto().id("1221313223"))
            .step(NOTIFICATION_TRACKER_STEP)
            .build();

    private static RequestDto buildRequestDto()
    {
        //RetryDto
        RetryDto retryDto=new RetryDto();
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        retryDto.setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        retryDto.setRetryStep(BigDecimal.valueOf(0));
        retryDto.setRetryPolicy(retries);

        //RequestMetadataDto
        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(retryDto);

        //RequestDto
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(EMAIL_PRESA_IN_CARICO_INFO.getRequestIdx());
        requestDto.setxPagopaExtchCxId(EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId());
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }

    @Test
    void gestionreRetryEmailAttachment_ShortTimeout(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getRequestIdx();

        var requestDto=buildRequestDto();
        requestDto.setRequestIdx(requestId);
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.delay(Duration.ofSeconds(1)).then(Mono.just(new ByteArrayOutputStream())));
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofMillis(1));

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message,QUEUE_NAME);
        StepVerifier.create(response).expectErrorMatches(throwable -> throwable instanceof TimeoutException).verify();
    }

    @Test
    void gestionreRetryEmailAttachment_LongTimeout(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getRequestIdx();

        var requestDto=buildRequestDto();
        requestDto.setRequestIdx(requestId);
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.delay(Duration.ofSeconds(1)).then(Mono.just(new ByteArrayOutputStream())));
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofSeconds(60));

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
    }

    @Test
    void gestionreRetryEmailAttachment_NoTimeout(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getRequestIdx();

        var requestDto=buildRequestDto();
        requestDto.setRequestIdx(requestId);
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.delay(Duration.ofSeconds(1)).then(Mono.just(new ByteArrayOutputStream())));
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(TIMEOUT_INACTIVE_DURATION);

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
    }

}
