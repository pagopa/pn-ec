package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.configurationproperties.RasterProperties;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.rest.call.upload.UploadCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.cartaceo.configuration.RasterConfiguration;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.consolidatore.utils.ContentTypes.APPLICATION_PDF;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.*;
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
    @SpyBean
    private RasterProperties rasterProperties;
    @Autowired
    private RasterConfiguration rasterConfiguration;

    private static final String DOWNLOAD_URL = "http://downloadUrl";

    private static final String UPLOAD_URL = "http://uploadUrl";

    private static final String SECRET = "secret";

    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequest(2)).build();

    private static CartaceoPresaInCaricoInfo getCartaceoPresaInCaricoInfo() {
        return getCartaceoPresaInCaricoInfo("requestPaId");
    }

    private static CartaceoPresaInCaricoInfo getCartaceoPresaInCaricoInfo(String requestPaId) {
        return CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequestPdfRaster(requestPaId)).build();
    }

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
    void lavorazioneRichiesta_PutRequestKo() {

        //WHEN
        mockGestoreRepository();
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.error(new RestCallException.ResourceAlreadyInProgressException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    /**
     * Test per la gestione di un errore permanente da parte del consolidatore.
     * Il messaggio associato alla richiesta viene cancellato dalla coda di lavorazione.
     */
    @Test
    void lavorazioneRichiesta_PutRequest_BadRequest_Acknowledge() {

        //WHEN
        mockGestoreRepository();
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(SYNTAX_ERROR_CODE)));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(SYNTAX_ERROR), any(PaperProgressStatusDto.class));
    }

    /**
     * Test per la gestione di un errore permanente da parte del consolidatore.
     * L'errore potrebbe essere risolvibile e quindi il messaggio associato alla richiesta viene mandato in DLQ.
     */
    @Test
    void lavorazioneRichiesta_PutRequest_BadRequest_ToDlq() {

        //WHEN
        mockGestoreRepository();
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(AUTHENTICATION_ERROR_CODE)));
        when(gestoreRepositoryCall.patchRichiesta(eq(DEFAULT_ID_CLIENT_HEADER_VALUE), eq(DEFAULT_REQUEST_IDX), any(PatchDto.class))).thenReturn(Mono.just(new RequestDto()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnDlqErrorQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO));
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(AUTHENTICATION_ERROR), any(PaperProgressStatusDto.class));
    }

    /**
     * Test per la gestione di un errore permanente da parte del consolidatore.
     * L'errore potrebbe essere risolvibile e quindi il messaggio associato alla richiesta viene mandato in DLQ.
     */
    @Test
    void lavorazioneRichiesta_PutRequest_PermanentError() {

        //WHEN
        mockGestoreRepository();
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.error(new ConsolidatoreException.PermanentException("permanent error")));
        when(gestoreRepositoryCall.patchRichiesta(eq(DEFAULT_ID_CLIENT_HEADER_VALUE), eq(DEFAULT_REQUEST_IDX), any(PatchDto.class))).thenReturn(Mono.just(new RequestDto()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnDlqErrorQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO));
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(ERROR.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    /**
     * Test per la gestione di un errore temporaneo da parte del consolidatore.
     * Il messaggio viene mandato nella coda di retry.
     */
    @Test
    void lavorazioneRichiesta_PutRequest_TemporaryError() {

        //WHEN
        mockGestoreRepository();
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.error(new ConsolidatoreException.TemporaryException("temporary error")));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    /**
     * Test lavorazione richiesta con step PDF Raster e configurazione PnECPaperPAIdToRaster = ALL (feature sempre abilitata).
     */
    @Test
    void lavorazioneRichiestaPdfRaster_All_Ok() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(rasterProperties.paIdToRaster()).thenReturn("ALL");

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
        verify(cartaceoService, never()).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));
    }

    /**
     * Test lavorazione richiesta con step PDF Raster e configurazione PnECPaperPAIdToRaster = NOTHING (feature sempre disabilitata).
     *
     */
    @Test
    void lavorazioneRichiestaPdfRaster_Nothing_Ok() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(rasterProperties.paIdToRaster()).thenReturn("NOTHING");

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(dynamoPdfRasterService, never()).insertRequestConversion(any(RequestConversionDto.class));
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));
    }

    /**
     * Test lavorazione richiesta con step PDF Raster e configurazione PnECPaperPAIdToRaster = <elenco di ID delle PA>.
     * Il requestPaId della richiesta è presente nella configurazione. La funzionalità è quindi abilitata.
     *
     */
    @Test
    void lavorazioneRichiestaPdfRaster_PaIdInList_Ok() {

        //GIVEN
        String requestPaIdToCheck = "validRequestPaId";
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo(requestPaIdToCheck);
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(rasterProperties.paIdToRaster()).thenReturn("requestPaId1;requestPaId2;" + requestPaIdToCheck);

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
        verify(cartaceoService, never()).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));
    }

    /**
     * Test lavorazione richiesta con step PDF Raster e configurazione PnECPaperPAIdToRaster = <elenco di ID delle PA>.
     * Il requestPaId della richiesta NON è presente nella configurazione. La funzionalità viene quindi disattivata per la specifica richiesta.
     *
     */
    @Test
    void lavorazioneRichiestaPdfRaster_PaIdNotInList_Ok() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(rasterProperties.paIdToRaster()).thenReturn("requestPaId1;requestPaId2");

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(dynamoPdfRasterService, never()).insertRequestConversion(any(RequestConversionDto.class));
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoGetFile() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(fileCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.error(new AttachmentNotAvailableException("fileKey")));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoDownloadCall() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(downloadCall.downloadFile(DOWNLOAD_URL)).thenReturn(Mono.error(new RuntimeException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoPostFile() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.error(new RuntimeException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoUploadCall() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(uploadCall.uploadFile(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(byte[].class))).thenReturn(Mono.error(new RuntimeException()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaPdfRaster_KoDynamo() {

        //GIVEN
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(dynamoPdfRasterService.insertRequestConversion(any())).thenReturn(Mono.error(DynamoDbException.builder().build()));

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaAlreadyInSent() {

        //WHEN
        mockGestoreRepository(SENT);
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(paperMessageCall, never()).putRequest(any());
        verify(cartaceoService, never()).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO), eq(CODE_TO_STATUS_MAP.get(OK_CODE)), any(PaperProgressStatusDto.class));

    }

    @Test
    void chooseStepTest(){
        //WHEN
        RequestDto requestDto = mockGestoreRepository(BOOKED);
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        StepError stepError = new StepError();
        stepError.setStep(StepError.StepErrorEnum.PUT_REQUEST_STEP);
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .stepError(stepError)
                .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequest(2)).build();

        //THEN
        Mono<SendMessageResponse> chooseStep = ReflectionTestUtils.invokeMethod(cartaceoService, "chooseStep", cartaceoPresaInCaricoInfo,
                new it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest(),
                new it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest(),
                requestDto);

        assert chooseStep != null;
        StepVerifier.create(chooseStep).expectNextCount(1).verifyComplete();
        verify(paperMessageCall, times(1)).putRequest(any());
    }

    @Test
    void chooseStepAlreadyInSent(){
        //WHEN
        RequestDto requestDto = mockGestoreRepository(SENT);
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        StepError stepError = new StepError();
        stepError.setStep(StepError.StepErrorEnum.PUT_REQUEST_STEP);
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .stepError(stepError)
                .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequest(2)).build();

        //THEN
        Mono<SendMessageResponse> chooseStep = ReflectionTestUtils.invokeMethod(cartaceoService, "chooseStep", cartaceoPresaInCaricoInfo,
                new it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest(),
                new it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest(),
                requestDto);


        assert chooseStep != null;
        StepVerifier.create(chooseStep).expectNextCount(1).verifyComplete();
        verify(paperMessageCall, never()).putRequest(any());
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

}