package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.exception.InvalidReceiverDigitalAddressException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.InvalidAttachmentSchemaException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sercq.model.pojo.SercqPresaInCaricoInfo;
import it.pagopa.pn.ec.sercq.service.impl.SercqService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.SERCQ;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
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

    private static final SercqPresaInCaricoInfo SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS = SercqPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createInvalidAddressDigitalNotificationRequest())
            .build();

    private static final SercqPresaInCaricoInfo SERCQ_PRESA_IN_CARICO_INFO_INVALID_ATTACHMENT = SercqPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createInvalidAttachmentDigitalNotificationRequest())
            .build();


    public static DigitalNotificationRequest createDigitalNotificationRequest() {
        return createDigitalNotificationRequest("x-pagopa-pn-sercq:send-self:notification-already-delivered?timestamp=" + Instant.now(), DEFAULT_ATTACHMENT_URL);
    }

    public static DigitalNotificationRequest createInvalidAttachmentDigitalNotificationRequest() {
        return createDigitalNotificationRequest("x-pagopa-pn-sercq:send-self:notification-already-delivered?timestamp=" + Instant.now(), DEFAULT_ATTACHMENT_URL_KO);
    }

    public static DigitalNotificationRequest createInvalidAddressDigitalNotificationRequest() {
        return createDigitalNotificationRequest("invalid-address", DEFAULT_ATTACHMENT_URL_KO);
    }

    public static DigitalNotificationRequest createDigitalNotificationRequest(String receiverDigitalAddress, String attachmentUrl) {
        DigitalNotificationRequest digitalNotificationRequest= new DigitalNotificationRequest();

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(attachmentUrl);

        digitalNotificationRequest.setRequestId("requestIdx");
        digitalNotificationRequest.eventType("string");
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress(receiverDigitalAddress);
        digitalNotificationRequest.setMessageText("string");
        digitalNotificationRequest.channel(SERCQ);
        digitalNotificationRequest.setSubjectText("prova testo");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentUrls(defaultListAttachmentUrls);
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
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO);
        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);

        StepVerifier.create(result)
               .verifyComplete();


        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                        .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true);

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()));

        verify(sercqService).sendNotificationOnStatusQueue(any(), eq(BOOKED.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(sercqService).sendNotificationOnStatusQueue(any(), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void testSpecificPresaInCaricoAllegatiKo(){

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(),any(),anyBoolean()))
                .thenReturn(Flux.error(new InvalidAttachmentSchemaException()));

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO_INVALID_ATTACHMENT);

        StepVerifier.create(result)
                .expectError(InvalidAttachmentSchemaException.class)
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
                .expectError(SqsClientException.class)
                .verify();

        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                        .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true);

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()));

        verify(sercqService).sendNotificationOnStatusQueue(any(), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));


    }

    @Test
    void testSpecificPresaInCaricoInvalidAddressKo(){

        when(sqsService.send(any(),any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        FileDownloadResponse mockedResponse = new FileDownloadResponse();
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS.getDigitalNotificationRequest()
                .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS.getXPagopaExtchCxId(), true))
                .thenReturn(Flux.just(mockedResponse));

        when(gestoreRepositoryCall.insertRichiesta(any()))
                .thenReturn(Mono.just(new RequestDto()));

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS);

        StepVerifier.create(result)
                .expectError(InvalidReceiverDigitalAddressException.class)
                .verify();

        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS.getDigitalNotificationRequest()
                        .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS.getXPagopaExtchCxId(), true);

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO_INVALID_ADDRESS.getXPagopaExtchCxId()));

        verify(sercqService).sendNotificationOnStatusQueue(any(), eq(ADDRESS_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        verify(sercqService, never())
                .sendNotificationOnStatusQueue(any(), eq(BOOKED.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void testGeneratedMessageDto() {
        FileDownloadResponse mockedResponse = new FileDownloadResponse();
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true))
                .thenReturn(Flux.just(mockedResponse));

        when(gestoreRepositoryCall.insertRichiesta(any()))
                .thenReturn(Mono.just(new RequestDto()));

        when(sqsService.send(any(), any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));

        Mono<Void> result = sercqService.specificPresaInCarico(SERCQ_PRESA_IN_CARICO_INFO);

        StepVerifier.create(result)
                .verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);

        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(SERCQ_PRESA_IN_CARICO_INFO.getDigitalNotificationRequest()
                        .getAttachmentUrls(), SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(), true);

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()));

        verify(sercqService).sendNotificationOnStatusQueue(any(), eq(BOOKED.getStatusTransactionTableCompliant()), captor.capture());
        verify(sercqService).sendNotificationOnStatusQueue(any(), eq(SENT.getStatusTransactionTableCompliant()), captor.capture());

        List<DigitalProgressStatusDto> capturedDtos = captor.getAllValues();
        Assertions.assertEquals(2, capturedDtos.size());

        DigitalProgressStatusDto bookedDto = capturedDtos.get(0);
        DigitalProgressStatusDto sentDto = capturedDtos.get(1);

        Assertions.assertTrue(Objects.nonNull(sentDto.getGeneratedMessage().getId()));
        Assertions.assertEquals("@send-self", sentDto.getGeneratedMessage().getSystem());
        Assertions.assertFalse(Objects.nonNull(bookedDto.getGeneratedMessage()));
    }

    }
