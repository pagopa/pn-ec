package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.configurationproperties.TransformationProperties;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.configuration.normalization.NormalizationConfiguration;
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
import it.pagopa.pn.ec.commons.service.AttachmentService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.cartaceo.configuration.PdfTransformationConfiguration;
import it.pagopa.pn.ec.pdfraster.service.RequestConversionService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private RequestConversionService requestConversionService;
    @SpyBean
    private TransformationProperties transformationProperties;
    @SpyBean
    private NormalizationConfiguration normalizationConfiguration;

    @SpyBean
    private AttachmentService attachmentService;

    @Autowired
    private PdfTransformationConfiguration pdfTransformationConfiguration;

    private static final String DOWNLOAD_URL = "http://downloadUrl";

    private static final String UPLOAD_URL = "http://uploadUrl";

    private static final String SECRET = "secret";

    private static final String DOCUMENT_TYPE_FOR_RASTERIZED = "PN_PAPER_ATTACHMENT";

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
        when(transformationProperties.paIdToRaster()).thenReturn("ALL");

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(requestConversionService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
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
        when(transformationProperties.paIdToRaster()).thenReturn("NOTHING");

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(requestConversionService, never()).insertRequestConversion(any(RequestConversionDto.class));
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
        when(transformationProperties.paIdToRaster()).thenReturn("requestPaId1;requestPaId2;" + requestPaIdToCheck);

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(requestConversionService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
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
        when(transformationProperties.paIdToRaster()).thenReturn("requestPaId1;requestPaId2");

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(requestConversionService, never()).insertRequestConversion(any(RequestConversionDto.class));
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
        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());

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
        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());

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

        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());


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
        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(uploadCall.uploadFile(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(byte[].class))).thenReturn(Mono.error(new RuntimeException()));
        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());

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
        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());

        //WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();
        when(requestConversionService.insertRequestConversion(any())).thenReturn(Mono.error(DynamoDbException.builder().build()));
        doReturn(true).when(cartaceoService).isRasterFeatureEnabled(anyString());

        //THEN
        Mono<SendMessageResponse> lavorazioneRichiesta = cartaceoService.lavorazioneRichiesta(cartaceoPresaInCaricoInfo);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(RETRY.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
    }

    /**
     * Verifica che lo step di trasformazione venga eseguito quando
     * il campo transformationDocumentType è valorizzato, anche se
     * la PA NON è abilitata né a raster né a normalizzazione.
     */
    @Test
    void lavorazioneRichiestaTransformation_ExplicitFlag_Ok() {

        // GIVEN
        CartaceoPresaInCaricoInfo cartaceoInfo = getCartaceoPresaInCaricoInfo();
        cartaceoInfo.getPaperEngageRequest()
                .setTransformationDocumentType("PN_CLEAN_PAPER_ATTACHMENT");     // flag esplicito per la normalizzazione
        cartaceoInfo.getPaperEngageRequest().setApplyRasterization(null);        // disabilito raster

        // Raster disabilitato per tutte le PA
        when(transformationProperties.paIdToRaster()).thenReturn("NOTHING");

        // Normalizzazione disabilitata (default paIdToNormalize = NOTHING)
        ReflectionTestUtils.setField(normalizationConfiguration, "paIdToNormalize", "NOTHING");

        // WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        Mono<SendMessageResponse> lavorazione = cartaceoService.lavorazioneRichiesta(cartaceoInfo);

        // THEN
        StepVerifier.create(lavorazione).expectNextCount(1).verifyComplete();

        // Deve aver creato la richiesta di conversione
        verify(requestConversionService, times(1))
                .insertRequestConversion(any(RequestConversionDto.class));
    }

    /**
     * La PA è abilitata alla NORMALIZZAZIONE tramite configurazione.
     * Nessun applyRasterization e nessun transformationDocumentType esplicito.
     */
    @Test
    void lavorazioneRichiestaTransformation_NormalizationEnabled_Ok() {
        /* GIVEN */
        CartaceoPresaInCaricoInfo info = getCartaceoPresaInCaricoInfo();
        info.getPaperEngageRequest().setApplyRasterization(null);                 // disabilito raster flag
        info.getPaperEngageRequest().setTransformationDocumentType(null);        // nessun override

        // Raster disabilitato
        when(transformationProperties.paIdToRaster()).thenReturn("NOTHING");

        // Normalizzazione ABILITATA per la PA (ALL o lista)
        ReflectionTestUtils.setField(normalizationConfiguration, "paIdToNormalize", "ALL");

        /* WHEN */
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        Mono<SendMessageResponse> lavorazione = cartaceoService.lavorazioneRichiesta(info);

        /* THEN */
        StepVerifier.create(lavorazione).expectNextCount(1).verifyComplete();
        verify(requestConversionService, times(1))
                .insertRequestConversion(any(RequestConversionDto.class));
    }

    /**
     * La PA è abilitata alla RASTERIZZAZIONE tramite configurazione.
     * Nessun applyRasterization e nessun transformationDocumentType esplicito.
     */
    @Test
    void lavorazioneRichiestaTransformation_RasterEnabled_Ok() {
        //GIVEN
        CartaceoPresaInCaricoInfo info = getCartaceoPresaInCaricoInfo();

        // Imposto la PA coerente con il mock
        info.getPaperEngageRequest().setRequestPaId("PA_TEST");
        info.getPaperEngageRequest().setApplyRasterization(null);
        info.getPaperEngageRequest().setTransformationDocumentType(null);

        when(transformationProperties.paIdToRaster()).thenReturn("PA_TEST");   // lista che contiene la stessa PA
        ReflectionTestUtils.setField(normalizationConfiguration, "paIdToNormalize", "NOTHING");

        // WHEN
        mockGestoreRepository();
        mockPutRequest();
        mockPdfRasterAttachmentSteps();

        Mono<SendMessageResponse> lavorazione = cartaceoService.lavorazioneRichiesta(info);

        // THEN
        StepVerifier.create(lavorazione).expectNextCount(1).verifyComplete();
        verify(requestConversionService, times(1))
                .insertRequestConversion(any(RequestConversionDto.class));
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

    @Test
    void specificPresaInCaricoInfoTest(){
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();
        mockPdfRasterAttachmentSteps();
        mockGestoreRepository();
        mockPutRequest();
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse().key("key").download(new FileDownloadInfo().url(DOWNLOAD_URL)).checksum("checksum").contentType("application/pdf")));
        when(gestoreRepositoryCall.insertRichiesta(any())).thenReturn(Mono.just(new RequestDto()));

        StepVerifier.create(cartaceoService.specificPresaInCarico(cartaceoPresaInCaricoInfo))
                .verifyComplete();

        Assertions.assertEquals(DOCUMENT_TYPE_FOR_RASTERIZED,cartaceoPresaInCaricoInfo.getPaperEngageRequest().getTransformationDocumentType());
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

    private CartaceoPresaInCaricoInfo createCartaceoPresaInCaricoInfo() {
        PaperEngageRequestAttachments paperEngageRequestAttachments = new PaperEngageRequestAttachments();
        PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
        paperEngageRequestAttachments.setUri("safestorage://prova.pdf");
        paperEngageRequestAttachments.setOrder(BigDecimal.valueOf(1));
        paperEngageRequestAttachments.setDocumentType("TEST");
        paperEngageRequestAttachments.setSha256("stringstringstringstringstringstringstri");
        List<PaperEngageRequestAttachments> paperEngageRequestAttachmentsList = new ArrayList<>();
        paperEngageRequestAttachmentsList.add(paperEngageRequestAttachments);
        paperEngageRequest.setAttachments(paperEngageRequestAttachmentsList);
        paperEngageRequest.setReceiverName("");
        paperEngageRequest.setReceiverNameRow2("");
        paperEngageRequest.setReceiverAddress("");
        paperEngageRequest.setReceiverAddressRow2("");
        paperEngageRequest.setReceiverCap("");
        paperEngageRequest.setReceiverCity("");
        paperEngageRequest.setReceiverCity2("");
        paperEngageRequest.setReceiverPr("");
        paperEngageRequest.setReceiverCountry("");
        paperEngageRequest.setReceiverFiscalCode("");
        paperEngageRequest.setSenderName("");
        paperEngageRequest.setSenderAddress("");
        paperEngageRequest.setSenderCity("");
        paperEngageRequest.setSenderPr("");
        paperEngageRequest.setSenderDigitalAddress("");
        paperEngageRequest.setArName("");
        paperEngageRequest.setArAddress("");
        paperEngageRequest.setArCap("");
        paperEngageRequest.setArCity("");
        var vas = new HashMap<String, String>();
        paperEngageRequest.setVas(vas);
        paperEngageRequest.setIun("iun123456789");
        paperEngageRequest.setRequestPaId("PagoPa");
        paperEngageRequest.setProductType("AR");
        paperEngageRequest.setPrintType("B/N12345");
        paperEngageRequest.setRequestId("requestIdx_1234567891234567891010");
        paperEngageRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        paperEngageRequest.setApplyRasterization(true);

        return CartaceoPresaInCaricoInfo.builder()
                .requestIdx("requestIdx")
                .xPagopaExtchCxId("xPagopaExtchCxId")
                .paperEngageRequest(paperEngageRequest)
                .build();
    }

    private void mockPutRequest() {
        when(paperMessageCall.putRequest(any())).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));
    }

}