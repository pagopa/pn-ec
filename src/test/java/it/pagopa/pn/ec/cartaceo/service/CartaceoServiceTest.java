package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic500ErrorException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.pdfraster.PdfRasterCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.rest.call.upload.UploadCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pdfraster.model.dto.PdfRasterResponse;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.RETRY;
import static it.pagopa.pn.ec.consolidatore.utils.ContentTypes.APPLICATION_PDF;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.CODE_TO_STATUS_MAP;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.OK_CODE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class CartaceoServiceTest {

    @SpyBean
    private CartaceoService cartaceoService;
    @MockBean
    private PaperMessageCall paperMessageCall;
    @MockBean
    private PdfRasterCall pdfRasterCall;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private DownloadCall downloadCall;
    @MockBean
    private UploadCall uploadCall;
    @MockBean
    private FileCall fileCall;
    @SpyBean
    private SqsService sqsService;
    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;
    @SpyBean
    private DynamoPdfRasterService dynamoPdfRasterService;

    private static final String DOWNLOAD_URL = "http://downloadUrl";

    private static final String UPLOAD_URL = "http://uploadUrl";

    private static final String SECRET = "secret";

    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequest(2)).build();

    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequestPdfRaster(2)).build();

    @Test
    void lavorazioneRichiestaOk() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));

    }

    @Test
    void lavorazioneRichiesta_GetRichiestaKo() {

        //WHEN
        when(gestoreRepositoryCall.getRichiesta(eq(DEFAULT_ID_CLIENT_HEADER_VALUE), eq(DEFAULT_REQUEST_IDX))).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiesta_PutRequestKo() {

        //WHEN
        mockGestoreRepository();
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.error(new RestCallException.ResourceAlreadyInProgressException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRasterOk() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        //Mock chiamata pdfRaster
        when(pdfRasterCall.convertPdf(any())).thenReturn(Mono.just(PdfRasterResponse.builder().newFileKey("").build()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
        verify(cartaceoService, never()).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));
    }

    @ParameterizedTest
    @MethodSource("exceptionsPdfRaster")
    void lavorazioneRichiestaKoPdfRaster(Exception e){
        when(gestoreRepositoryCall.getRichiesta(any(), any())).thenReturn(Mono.just(new RequestDto()));

        //Mock chiamata pdfRaster
        when(pdfRasterCall.convertPdf(any())).thenReturn(Mono.error(e));

        Mono<SendMessageResponse> lavorazioneRichiesta=cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);

        StepVerifier.create(lavorazioneRichiesta)
                    .expectNextCount(1)
                    .verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoGetFile() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(fileCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.error(new AttachmentNotAvailableException("fileKey")));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoDownloadCall() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(downloadCall.downloadFile(DOWNLOAD_URL)).thenReturn(Mono.error(new RuntimeException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoPostFile() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.error(new RuntimeException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoUploadCall() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(uploadCall.uploadFile(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(byte[].class))).thenReturn(Mono.error(new RuntimeException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoDynamo() {

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(dynamoPdfRasterService.insertRequestConversion(any())).thenReturn(Mono.error(DynamoDbException.builder().build()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    private void mockPdfRasterAttachmentSteps() {
        String originalFileKey = randomAlphanumeric(10);
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo().url(DOWNLOAD_URL);
        when(fileCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse().key(originalFileKey).download(fileDownloadInfo).checksum("checksum")));

        when(downloadCall.downloadFile(DOWNLOAD_URL)).thenReturn(Mono.just(new ByteArrayOutputStream()));

        String newFileKey = randomAlphanumeric(10);
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse().key(newFileKey).secret(SECRET).uploadUrl(UPLOAD_URL)));

        when(uploadCall.uploadFile(eq(newFileKey), eq(UPLOAD_URL), eq(SECRET), eq(APPLICATION_PDF), eq(DocumentTypeConfiguration.ChecksumEnum.SHA256), anyString(), any(byte[].class))).thenReturn(Mono.empty());
    }

    private void mockGestoreRepository() {
        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(eq(DEFAULT_ID_CLIENT_HEADER_VALUE), eq(DEFAULT_REQUEST_IDX))).thenReturn(Mono.just(new RequestDto()));
    }

    private void mockPutRequest() {
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));
    }

    /**
     *
     * @return
     */
    private static Stream<Arguments> exceptionsPdfRaster(){
        return Stream.of(
                Arguments.of(new AttachmentNotAvailableException("")),
                Arguments.of(new ClientNotAuthorizedException("")),
                Arguments.of(new Generic400ErrorException("","")),
                Arguments.of(new Generic500ErrorException("",""))
        );
    }
}