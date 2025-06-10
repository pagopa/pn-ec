package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.rest.call.upload.UploadCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sqs.SqsTimeoutProvider;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.consolidatore.utils.ContentTypes.APPLICATION_PDF;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.OK_CODE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv
@DirtiesContext
class CartaceoServiceTimeoutTest {

    @SpyBean
    private CartaceoService cartaceoService;
    @MockBean
    private PaperMessageCall paperMessageCall;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private DownloadCall downloadCall;
    @MockBean
    private UploadCall uploadCall;
    @MockBean
    private FileCall fileCall;
    @MockBean
    private SqsTimeoutProvider sqsTimeoutProvider;
    @SpyBean
    private SqsService sqsService;

    private static final String DOWNLOAD_URL = "http://downloadUrl";
    private static final String UPLOAD_URL = "http://uploadUrl";
    private static final String SECRET = "secret";
    private static final Duration TIMEOUT_INACTIVE_DURATION = Duration.ofSeconds(86400);


    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequest(2)).build();


    @Test
    void lavorazioneRichiestaShortTimeout() {

        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofSeconds(1));

        doReturn(Mono.just("result")
                .delayElement(Duration.ofSeconds(2)))
                .when(cartaceoService)
                .chooseStep(any(CartaceoPresaInCaricoInfo.class),any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class),any(PaperEngageRequest.class),any(RequestDto.class));

        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectErrorMatches(throwable -> throwable instanceof TimeoutException).verify();

    }

    @Test
    void lavorazioneRichiestaNoTimeout() {

        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(TIMEOUT_INACTIVE_DURATION);

        doReturn(Mono.just(mockResponse)
                .delayElement(Duration.ofSeconds(2)))
                .when(cartaceoService)
                .chooseStep(any(CartaceoPresaInCaricoInfo.class),any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class),any(PaperEngageRequest.class),any(RequestDto.class));

        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

    }

    @Test
    void lavorazioneRichiestaLongTimeout() {

        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofSeconds(10));

        doReturn(Mono.just(mockResponse)
                .delayElement(Duration.ofSeconds(2)))
                .when(cartaceoService)
                .chooseStep(any(CartaceoPresaInCaricoInfo.class),any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class),any(PaperEngageRequest.class),any(RequestDto.class));

        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextMatches(response -> response.equals(mockResponse)).verifyComplete();

    }

    @Test
    void gestioneRetryCartaceoShortTimeout() {
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        Message mockMessage = Message.builder().receiptHandle("receipt").build();

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);

        when(sqsService.deleteMessageFromQueue(any(Message.class),anyString()))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));


        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofMillis(1));

        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())
                .delayElement(Duration.ofSeconds(2)))
                .when(sqsService)
                .changeMessageVisibility(anyString(),anyInt(),anyString());


        doReturn(Mono.just(mockResponse))
                .when(cartaceoService)
                .chooseStep(any(CartaceoPresaInCaricoInfo.class),any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class),any(PaperEngageRequest.class),any(RequestDto.class));

        Mono<SqsResponse> result = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO, mockMessage);
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof TimeoutException)
                .verify();
    }

    @Test
    void gestioneCartaceoNoTimeout(){
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        Message mockMessage = Message.builder().receiptHandle("receipt").build();

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);

        when(sqsService.deleteMessageFromQueue(any(Message.class),anyString()))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));


        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(TIMEOUT_INACTIVE_DURATION);

        doReturn(Mono.just(mockResponse))
                .when(cartaceoService)
                .chooseStep(any(CartaceoPresaInCaricoInfo.class),any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class),any(PaperEngageRequest.class),any(RequestDto.class));

        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())
                .delayElement(Duration.ofSeconds(2)))
                .when(sqsService)
                .changeMessageVisibility(anyString(),anyInt(),anyString());

        Mono<SqsResponse> result = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO, mockMessage);
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(sqsService,times(1)).changeMessageVisibility(anyString(),anyInt(),anyString());
    }

    @Test
    void gestioneCartaceoLongTimeout(){
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        Message mockMessage = Message.builder().receiptHandle("receipt").build();

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);

        when(sqsService.deleteMessageFromQueue(any(Message.class),anyString()))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));


        when(sqsTimeoutProvider.getTimeoutForQueue(any()))
                .thenReturn(Duration.ofSeconds(100));

        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())
                .delayElement(Duration.ofSeconds(2)))
                .when(sqsService)
                .changeMessageVisibility(anyString(),anyInt(),anyString());


        doReturn(Mono.just(mockResponse))
                .when(cartaceoService)
                .chooseStep(any(CartaceoPresaInCaricoInfo.class),any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class),any(PaperEngageRequest.class),any(RequestDto.class));

        Mono<SqsResponse> result = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO, mockMessage);
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    private void mockGestoreRepository() {
        mockGestoreRepository(BOOKED);
    }

    private RequestDto mockGestoreRepository(Status status) {
        // Mock di una generica getRichiesta.
        RequestDto requestDto = new RequestDto();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
        paperProgressStatusDto.status(status.getStatusTransactionTableCompliant());
        EventsDto eventsDto = new EventsDto();
        eventsDto.setPaperProgrStatus(paperProgressStatusDto);
        RequestMetadataDto requestMetadata = new RequestMetadataDto().paperRequestMetadata(new PaperRequestMetadataDto().requestPaId("requestPaId")).eventsList(List.of(eventsDto));

        requestDto.requestMetadata(requestMetadata);
        when(gestoreRepositoryCall.getRichiesta(eq(DEFAULT_ID_CLIENT_HEADER_VALUE), eq(DEFAULT_REQUEST_IDX))).thenReturn(Mono.just(requestDto));
        return requestDto;
    }

    private void mockPutRequest() {
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));
    }

    private void mockPdfRasterAttachmentSteps() {
        String originalFileKey = randomAlphanumeric(10);
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo().url(DOWNLOAD_URL);
        when(fileCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse().key(originalFileKey).download(fileDownloadInfo).checksum("checksum").contentType("application/pdf")));

        when(downloadCall.downloadFile(DOWNLOAD_URL)).thenReturn(Mono.just(new ByteArrayOutputStream()));

        String newFileKey = randomAlphanumeric(10);
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse().key(newFileKey).secret(SECRET).uploadUrl(UPLOAD_URL)));

        when(uploadCall.uploadFile(eq(newFileKey), eq(UPLOAD_URL), eq(SECRET), eq(APPLICATION_PDF), eq(DocumentTypeConfiguration.ChecksumEnum.SHA256), anyString(), any(byte[].class))).thenReturn(Mono.empty());
    }

}
