package it.pagopa.pn.ec.pec.service.impl;


import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.*;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.AlternativeProviderService;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pagopa.pn.library.pec.service.impl.PnPecServiceImpl;
import lombok.CustomLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.pec.service.impl.PecServiceTest.createDigitalNotificationRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class PnPecServiceTest {

    @MockBean
    private AttachmentServiceImpl attachmentService;
    @MockBean
    private DownloadCall downloadCall;
    @MockBean(name = "arubaServiceImpl")
    private ArubaService arubaService;
    @MockBean(name = "alternativeProviderServiceImpl")
    private AlternativeProviderService otherProviderService;
    @Autowired
    @Qualifier("pnPecServiceImpl")
    private PnPecService pnPecService;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @SpyBean
    private PecService pecService;
    @SpyBean
    private PnPecConfigurationProperties pnPecConfigurationProperties;

    private String PROVIDER_SWITCH_READ_DEFAULT = "1970-01-01T00:00:00Z;aruba";
    private String PROVIDER_SWITCH_WRITE_DEFAULT = "1970-01-01T00:00:00Z;aruba";
    private final String TEMPORARY_EXCEPTION = "test temporary exception";
    private final String PERMANENT_EXCEPTION = "test permanent exception";
    private final String MESSAGE = "test message";
    //private final String DATE_ALTERNATIVE = "2020-01-01T10:00:00Z";
    //private final String DATE_ARUBA = "2023-01-01T10:00:00Z";
    private final String ARUBA_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@pec.aruba.it";
    private final String ALTERNATIVE_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@pec.aruba.it.test";

    private final String DATE_R_ARUBA_W_ARUBA = "2022-12-02T00:00:00Z";
    private final String DATE_R_ARUBA_OTHER_W_OTHER = "2023-01-02T23:59:58Z";
    private final String DATE_R_OTHER_W_ARUBA = "2023-02-02T00:00:00Z";
    private final String DATE_DEFAULT = "1970-01-01T00:00:00Z";


    private final PnSpapiPermanentErrorException permanentException = new PnSpapiPermanentErrorException(PERMANENT_EXCEPTION);
    private final PnSpapiTemporaryErrorException temporaryException = new PnSpapiTemporaryErrorException(TEMPORARY_EXCEPTION);
    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO = PecPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX).xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE).digitalNotificationRequest(createDigitalNotificationRequest()).build();

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).getMillis());
        PROVIDER_SWITCH_READ_DEFAULT = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "pnPecProviderSwitchRead");
        PROVIDER_SWITCH_WRITE_DEFAULT = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite");
    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchRead", PROVIDER_SWITCH_READ_DEFAULT);
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", PROVIDER_SWITCH_WRITE_DEFAULT);
    }

    @AfterAll
    static void afterAll() {
        DateTimeUtils.setCurrentMillisSystem();
    }


    @Nested
    class sendMailTests {

        @Test
        void sendMailExpectArubaOK() {
            log.debug("sendMailExpectArubaOk");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_W_ARUBA).getMillis());
            var requestDto = buildRequestDto();
            var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
            var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

            when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
            when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
            when(arubaService.sendMail(any())).thenReturn(Mono.just(MESSAGE));
            when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

            Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
            StepVerifier.create(response).expectNextCount(1).verifyComplete();

            verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
            verify(arubaService, times(1)).sendMail(any());
            verify(otherProviderService, never()).sendMail(any());
        }

        @Test
        void sendMailExpectArubaKo() {
            log.debug("sendMailExpectArubaKo");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).getMillis());
            var requestDto = buildRequestDto();
            var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
            var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

            when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
            when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
            when(arubaService.sendMail(any())).thenReturn(Mono.error(permanentException));
            when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

            Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
            StepVerifier.create(response).expectError(RuntimeException.class).verify();

            verify(pecService, times(0)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
            verify(arubaService, times(1)).sendMail(any());
            verify(otherProviderService, never()).sendMail(any());
        }


        @Test
        void sendMailExpectAlternativeOK() {
            log.debug("sendMailExpectAlternative");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());
            var requestDto = buildRequestDto();
            var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
            var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

            when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
            when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
            when(arubaService.sendMail(any())).thenReturn(Mono.just(MESSAGE));
            when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

            Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
            StepVerifier.create(response).expectNextCount(1).verifyComplete();

            verify(pecService, times(0)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
            verify(arubaService, never()).sendMail(any());
            //TODO modificare test una volta implementato il nuovo provider
            verify(otherProviderService, times(1)).sendMail(any());
        }

        @Test
        void sendMailExpectAlternativeKo() {
            log.debug("sendMailExpectAlternativeKo");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_OTHER_W_ARUBA).getMillis());
            var requestDto = buildRequestDto();
            var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
            var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

            when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
            when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
            when(arubaService.sendMail(any())).thenReturn(Mono.error(permanentException));
            when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

            Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
            StepVerifier.create(response).expectError(RuntimeException.class).verify();

            verify(pecService, times(0)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
            verify(arubaService, never()).sendMail(any());
            verify(otherProviderService, times(1)).sendMail(any());
        }

        @Test
        void sendMailArubaPermanentException() {
            log.debug("sendMailArubaPermanentException");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_OTHER_W_ARUBA).getMillis());

            when(arubaService.sendMail(any())).thenReturn(Mono.error(permanentException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).sendMail(any());
            verify(otherProviderService, never()).sendMail(any());
        }

        @Test
        void sendMailOtherServicePermanentException() {
            log.debug("sendMailOtherServicePermanentException");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());

            when(otherProviderService.sendMail(any())).thenReturn(Mono.error(permanentException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, never()).sendMail(any());
            verify(otherProviderService, times(1)).sendMail(any());
        }

        @Test
        void sendMailWrongProvider() {
            log.debug("sendMailWrongProvider");
            ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", "wrongProvider");

            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_OTHER_W_ARUBA).getMillis());
            var requestDto = buildRequestDto();
            var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
            var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

            when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
            when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
            when(arubaService.sendMail(any())).thenReturn(Mono.just(MESSAGE));
            when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

            Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
            StepVerifier.create(response).expectNextCount(1).expectComplete().verify();

            verify(pecService, times(0)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
            verify(arubaService, never()).sendMail(any());
            //TODO modificare test una volta implementato il nuovo provider
            verify(otherProviderService, times(0)).sendMail(any());
        }

        @Test
        void sendMailArubaRetriesExceeded() {
            log.debug("sendMailArubaRetriesExceeded");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_W_ARUBA).getMillis());

            when(arubaService.sendMail(any())).thenReturn(Mono.error(temporaryException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).sendMail(any());
            verify(otherProviderService, never()).sendMail(any());
        }

        @Test
        void sendMailOtherServiceRetriesExceeded() {
            log.debug("sendMailArubaRetriesExceeded");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());

            when(otherProviderService.sendMail(any())).thenReturn(Mono.error(temporaryException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, never()).sendMail(any());
            verify(otherProviderService, times(1)).sendMail(any());
        }
    }

    @Nested
    class getUnreadMessagesTests {
        @Test
        void getUnreadMessagesExpectBothOk() {
            log.debug("getUnreadMessagesExpectBothOk");

            PnGetMessagesResponse arubaMessages = getArubaMessage();
            PnGetMessagesResponse otherProviderMessages = getOtherProviderMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.just(otherProviderMessages));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages).expectNextMatches(response ->
                            response.getNumOfMessages() == arubaMessages.getNumOfMessages() + otherProviderMessages.getNumOfMessages()
                                    && response.getPnListOfMessages().getMessages().size() == arubaMessages.getPnListOfMessages().getMessages().size() + otherProviderMessages.getPnListOfMessages().getMessages().size())
                    .expectComplete()
                    .verify();
            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);

        }

        @Test
        void getUnreadMessagesOtherProviderKo() {
            log.debug("getUnreadMessagesOtherProviderKo");

            PnGetMessagesResponse arubaMessages = getArubaMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectNextMatches(response -> response.getNumOfMessages() == arubaMessages.getNumOfMessages()
                            && response.getPnListOfMessages().getMessages().size() == arubaMessages.getPnListOfMessages().getMessages().size())
                    .expectComplete()
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesArubaKo() {
            log.debug("getUnreadMessagesArubaKo");
            PnGetMessagesResponse otherProviderMessages = getOtherProviderMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.just(otherProviderMessages));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectNextMatches(response -> response.getNumOfMessages() == otherProviderMessages.getNumOfMessages()
                            && response.getPnListOfMessages().getMessages().size() == otherProviderMessages.getPnListOfMessages().getMessages().size())
                    .expectComplete()
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesBothKo() {
            log.debug("getUnreadMessagesBothKo");

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(ProvidersNotAvailableException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMesagesBothEmpty() {
            log.debug("getUnreadMessagesBothEmpty");
            PnGetMessagesResponse arubaMessages = new PnGetMessagesResponse();
            PnGetMessagesResponse otherProviderMessages = new PnGetMessagesResponse();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.just(otherProviderMessages));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectNextMatches(response -> response.getNumOfMessages() == 0
                            && response.getPnListOfMessages() == null)
                    .expectComplete()
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesOtherProviderRetriesExceeded() {
            log.debug("getUnreadMessagesOtherProviderRetriesExceeded");
            PnGetMessagesResponse arubaMessages = getArubaMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectNextMatches(response -> response.getNumOfMessages() == arubaMessages.getNumOfMessages()
                            && response.getPnListOfMessages().getMessages().size() == arubaMessages.getPnListOfMessages().getMessages().size())
                    .expectComplete()
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesArubaRetriesExceeded() {
            log.debug("getUnreadMessagesArubaRetriesExceeded");
            PnGetMessagesResponse otherProviderMessages = getOtherProviderMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.just(otherProviderMessages));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectNextMatches(response -> response.getNumOfMessages() == otherProviderMessages.getNumOfMessages()
                            && response.getPnListOfMessages().getMessages().size() == otherProviderMessages.getPnListOfMessages().getMessages().size())
                    .expectComplete()
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesBothRetriesExceeded() {
            log.debug("getUnreadMessagesOtherProviderKo");
            PnGetMessagesResponse arubaMessages = getArubaMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));

            Mono<PnGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(ProvidersNotAvailableException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(otherProviderService, times(1)).getUnreadMessages(6);
        }
    }

    @Nested
    class getMessageCountTests {

        @Test
        void getMessageCountExpectBothOk() {
            log.debug("getUnreadMessagesExpectBoth");
            when(arubaService.getMessageCount()).thenReturn(Mono.just(3));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.just(3));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectNext(6).expectComplete().verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(1)).getMessageCount();
        }

        @Test
        void getMessageCountOtherProviderKo() {
            log.debug("getUnreadMessagesOtherProviderKo");

            when(arubaService.getMessageCount()).thenReturn(Mono.just(3));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.error(permanentException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectNext(3).expectComplete().verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(1)).getMessageCount();
        }

        @Test
        void getMessageCountArubaKo() {
            log.debug("getUnreadMessagesArubaKo");

            when(arubaService.getMessageCount()).thenReturn(Mono.error(permanentException));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.just(3));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectNext(3).expectComplete().verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(1)).getMessageCount();
        }

        @Test
        void getMessageCountBothKo() {
            log.debug("getUnreadMessagesBothKo");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());

            when(arubaService.getMessageCount()).thenReturn(Mono.error(permanentException));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.error(permanentException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(0)).getMessageCount();
        }

        @Test
        void getMessageCountArubaRetriesExceeded() {
            log.debug("getUnreadMessagesArubaRetriesExceeded");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());

            when(arubaService.getMessageCount()).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.just(3));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(ArubaCallMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(1)).getMessageCount();
        }

        @Test
        void getMessageCountOtherProviderRetriesExceeded() {
            log.debug("getUnreadMessagesOtherProviderRetriesExceeded");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());


            when(arubaService.getMessageCount()).thenReturn(Mono.just(3));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.error(temporaryException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(AlternativeProviderMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(1)).getMessageCount();
        }

        @Test
        void getMessageCountBothRetriesExceeded() {
            log.debug("getUnreadMessagesBothRetry");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());

            when(arubaService.getMessageCount()).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.getMessageCount()).thenReturn(Mono.error(temporaryException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(ProvidersNotAvailableException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(otherProviderService, times(1)).getMessageCount();
        }
    }

    @Nested
    class MarkMessageAsReadTests {
        @Test
        void markMessageAsReadFromArubaOk() {
            log.debug("markMessageAsReadFromArubaOk");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());
            when(otherProviderService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(otherProviderService, never()).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromAlternativeOk() {
            log.debug("markMessageAsReadFromAlternativeOk");

            when(arubaService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(otherProviderService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, never()).markMessageAsRead(ALTERNATIVE_MESSAGE_ID);
            verify(otherProviderService, times(1)).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromArubaKo() {
            log.debug("markMessageAsReadFromArubaKo");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(otherProviderService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(otherProviderService, never()).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromAlternativeKo() {
            log.debug("markMessageAsReadFromAlternativeKo");

            when(arubaService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, never()).markMessageAsRead(ALTERNATIVE_MESSAGE_ID);
            verify(otherProviderService, times(1)).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromArubaRetriesExceeded() {
            log.debug("markMessageAsReadFromArubaRetriesExceeded");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID)).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(otherProviderService, never()).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromAlternativeRetriesExceeded() {
            log.debug("markMessageAsReadFromAlternativeRetriesExceeded");

            when(arubaService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(otherProviderService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.markMessageAsRead(ALTERNATIVE_MESSAGE_ID)).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, never()).markMessageAsRead(ALTERNATIVE_MESSAGE_ID);
            verify(otherProviderService, times(1)).markMessageAsRead(anyString());
        }
    }

    @Nested
    class DeleteMessageTests {
        @Test
        void deleteMessageFromArubaOk() {
            log.debug("deleteMessageFromArubaOk");

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());
            when(otherProviderService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(otherProviderService, never()).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromAlternativeOk() {
            log.debug("deleteMessageFromAlternativeOk");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());

            when(arubaService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.empty());
            when(otherProviderService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, never()).deleteMessage(ALTERNATIVE_MESSAGE_ID);
            verify(otherProviderService, times(1)).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromArubaKo() {
            log.debug("deleteMessageFromArubaKo");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_OTHER_W_ARUBA).getMillis());

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(otherProviderService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(otherProviderService, never()).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromAlternativeKo() {
            log.debug("deleteMessageFromAlternativeKo");

            when(arubaService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(permanentException));


            StepVerifier.create(pnPecService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, never()).deleteMessage(ALTERNATIVE_MESSAGE_ID);
            verify(otherProviderService, times(1)).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromArubaRetriesExceeded() {
            log.debug("deleteMessageFromArubaRetriesExceeded");

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(otherProviderService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID)).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(otherProviderService, never()).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromAlternativeRetriesExceeded() {
            log.debug("deleteMessageFromAlternativeRetriesExceeded");

            when(arubaService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(otherProviderService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.deleteMessage(ALTERNATIVE_MESSAGE_ID)).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, never()).deleteMessage(ALTERNATIVE_MESSAGE_ID);
            verify(otherProviderService, times(1)).deleteMessage(anyString());
        }
    }

    @Test
    void isArubaTest() {
        log.debug("isArubaTest");
        List<String> messageIDs = new ArrayList<>();
        messageIDs.add(ARUBA_MESSAGE_ID);
        messageIDs.add("@pec.aruba.it ");
        messageIDs.add("@PEC.ARUBA.IT");
        messageIDs.add("opec21010.20231006185001.00057.206.1.59@test.com");
        messageIDs.add(ALTERNATIVE_MESSAGE_ID);

        Assertions.assertTrue(PnPecServiceImpl.isAruba(messageIDs.get(0)));
        Assertions.assertTrue(PnPecServiceImpl.isAruba(messageIDs.get(1)));
        Assertions.assertTrue(PnPecServiceImpl.isAruba(messageIDs.get(2)));
        Assertions.assertFalse(PnPecServiceImpl.isAruba(messageIDs.get(3)));
        Assertions.assertFalse(PnPecServiceImpl.isAruba(messageIDs.get(4)));

    }

    private static RequestDto buildRequestDto() {
        RetryDto retryDto = new RetryDto();
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        retryDto.setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        retryDto.setRetryStep(BigDecimal.valueOf(0));
        retryDto.setRetryPolicy(retries);

        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(retryDto);

        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(PEC_PRESA_IN_CARICO_INFO.getRequestIdx());
        requestDto.setxPagopaExtchCxId(PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId());
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }

    private PnGetMessagesResponse getArubaMessage() {
        List<byte[]> arubaMessages = new ArrayList<>();
        arubaMessages.add("message5".getBytes());
        arubaMessages.add("message6".getBytes());
        arubaMessages.add("message7".getBytes());

        PnListOfMessages pnArubaListOfMessages = new PnListOfMessages();
        pnArubaListOfMessages.setMessages(arubaMessages);

        PnGetMessagesResponse arubaMessagesResponse = new PnGetMessagesResponse();
        arubaMessagesResponse.setPnListOfMessages(pnArubaListOfMessages);
        arubaMessagesResponse.setNumOfMessages(3);
        return arubaMessagesResponse;
    }

    private PnGetMessagesResponse getOtherProviderMessage() {
        List<byte[]> otherProviderMessages = new ArrayList<>();
        otherProviderMessages.add("message1".getBytes());
        otherProviderMessages.add("message2".getBytes());
        otherProviderMessages.add("message3".getBytes());

        PnGetMessagesResponse otherProviderMessagesResponse = new PnGetMessagesResponse();
        otherProviderMessagesResponse.setPnListOfMessages(new PnListOfMessages(otherProviderMessages));
        otherProviderMessagesResponse.setNumOfMessages(3);
        return otherProviderMessagesResponse;
    }

}