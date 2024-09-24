package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.InvalidAttachmentSchemaException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sercq.model.pojo.SercqPresaInCaricoInfo;
import it.pagopa.pn.ec.sercq.service.impl.SercqService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.SERCQ;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SercqServiceTest {

    @MockBean
    AttachmentServiceImpl attachmentService;

    @MockBean
    GestoreRepositoryCall gestoreRepositoryCall;

    @MockBean
    private SqsService sqsService;

    @SpyBean
    private SercqService sercqService;


    private static final String DEFAULT_ATTACHMENT_URL = "safestorage://prova.pdf";

    private static final String DEFAULT_ATTACHMENT_URL_KO = "prova.pdf";



    private static final SercqPresaInCaricoInfo SERCQ_PRESA_IN_CARICO_INFO = SercqPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createDigitalNotificationRequest())
            .build();

    private static final SercqPresaInCaricoInfo SERCQ_PRESA_IN_CARICO_INFO_KO = SercqPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createInvalidDigitalNotificationRequestKO())
            .build();


    public static DigitalNotificationRequest createDigitalNotificationRequest() {
        DigitalNotificationRequest digitalNotificationRequest= new DigitalNotificationRequest();

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(DEFAULT_ATTACHMENT_URL);

        digitalNotificationRequest.setRequestId("requestIdx");
        digitalNotificationRequest.eventType("string");
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress("x-pagopa-pn-sercq:SEND-self:notification-already-delivered?timestamp=" + Instant.now());
        digitalNotificationRequest.setMessageText("string");
        digitalNotificationRequest.channel(SERCQ);
        digitalNotificationRequest.setSubjectText("prova testo");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentUrls(defaultListAttachmentUrls);
        return digitalNotificationRequest;
    }

    public static DigitalNotificationRequest createInvalidDigitalNotificationRequestKO() {
        DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();

        List<String> invalidListAttachmentUrls = new ArrayList<>();
        invalidListAttachmentUrls.add(DEFAULT_ATTACHMENT_URL_KO);

        digitalNotificationRequest.setRequestId("");
        digitalNotificationRequest.eventType("");
        digitalNotificationRequest.setClientRequestTimeStamp(null);
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress("test-test");
        digitalNotificationRequest.setMessageText("test");
        digitalNotificationRequest.channel(SERCQ);
        digitalNotificationRequest.setSubjectText("");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentUrls(invalidListAttachmentUrls);

        return digitalNotificationRequest;
    }


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSpecificPresaInCarico() {

        FileDownloadResponse mockedResponse = new FileDownloadResponse();
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true))
                .thenReturn(Flux.just(mockedResponse));

        when(gestoreRepositoryCall.insertRichiesta(any()))
                .thenReturn(Mono.just(new RequestDto()));

        when(sqsService.send(any(),any()))
                .thenReturn(Mono.empty());

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO);

        StepVerifier.create(result)
               .verifyComplete();

        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                        .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true);

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()));

        verify(sqsService, times(1))
                .send(any(),any());
    }

    @Test
    void testSpecificPresaInCaricoAllegatiKo(){

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(),any(),anyBoolean()))
                .thenReturn(Flux.error(new InvalidAttachmentSchemaException()));

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO_KO);

        StepVerifier.create(result)
                .expectError(Exceptions.retryExhausted("", null).getClass())
                .verify();

        verifyNoInteractions(gestoreRepositoryCall);
        verifyNoInteractions(sqsService);

    }

    @Test
    void testSpecificPresaInCaricoSqsSendKo(){

        when(sqsService.send(any(),any()))
                .thenReturn(Mono.error(new SqsClientException("")));

        FileDownloadResponse mockedResponse = new FileDownloadResponse();
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true))
                .thenReturn(Flux.just(mockedResponse));

        when(gestoreRepositoryCall.insertRichiesta(any()))
                .thenReturn(Mono.just(new RequestDto()));

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO);

        StepVerifier.create(result)
                .expectError(Exceptions.retryExhausted("", null).getClass())
                .verify();

        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                        .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true);

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()));

        verify(sqsService, times(1))
                .send(any(),any());


    }


    }