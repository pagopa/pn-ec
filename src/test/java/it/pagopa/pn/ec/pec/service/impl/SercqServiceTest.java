package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AttachmentService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sercq.service.impl.SercqService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
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

    @Mock
    private AttachmentService attachmentService;

    @Mock
    GestoreRepositoryCall gestoreRepositoryCall;

    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";


    private static final PecPresaInCaricoInfo SERCQ_PRESA_IN_CARICO_INFO = PecPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createDigitalNotificationRequest())
            .build();


    public static DigitalNotificationRequest createDigitalNotificationRequest() {
        DigitalNotificationRequest digitalNotificationRequest= new DigitalNotificationRequest();

        //requestDto.setRequestIdx("requestIdx");

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(defaultAttachmentUrl);

        digitalNotificationRequest.setRequestId("requestIdx");
        digitalNotificationRequest.eventType("string");
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress("pippo@pec.it");
        digitalNotificationRequest.setMessageText("string");
        digitalNotificationRequest.channel(SERCQ);
        digitalNotificationRequest.setSubjectText("prova testo");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentUrls(defaultListAttachmentUrls);
        return digitalNotificationRequest;
    }

    @InjectMocks
    private SercqService sercqService;

    @Mock
    private SqsService sqsService;


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
                .expectNextCount(1).verifyComplete();

        verify(attachmentService, times(1))
                .getAllegatiPresignedUrlOrMetadata(anyString(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()), eq(true));

        verify(sercqService, times(1))
                .insertRequestFromSercq(any(), eq(SERCQ_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId()));

        verify(sercqService, times(1))
                .sendNotificationOnStatusQueue(SERCQ_PRESA_IN_CARICO_INFO, anyString(), new DigitalProgressStatusDto());
    }




}
