package it.pagopa.pn.ec.pec.service.impl;


import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.*;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import it.pagopa.pn.library.pec.service.impl.PnEcPecServiceImpl;
import lombok.CustomLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.pec.service.impl.PecServiceTest.createDigitalNotificationRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.ARUBA_PROVIDER;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.NAMIRIAL_PROVIDER;
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
    @MockBean
    private com.namirial.pec.library.service.PnPecServiceImpl namirialService;
    @Autowired
    private PnEcPecService pnPecService;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @SpyBean
    private PecService pecService;
    @SpyBean
    private PnPecConfigurationProperties pnPecConfigurationProperties;
    @SpyBean
    private CloudWatchPecMetrics cloudWatchPecMetrics;
    @Value("${library.pec.cloudwatch.namespace.aruba}")
    private String arubaProviderNamespace;
    @Value("${library.pec.cloudwatch.namespace.namirial}")
    private String namirialProviderNamespace;
    @Value("${library.pec.cloudwatch.metric.response-time.mark-message-as-read}")
    private String markMessageAsReadResponseTimeMetric;
    @Value("${library.pec.cloudwatch.metric.response-time.delete-message}")
    private String deleteMessageResponseTimeMetric;

    private String PROVIDER_SWITCH_READ_DEFAULT = "1970-01-01T00:00:00Z;aruba";
    private String PROVIDER_SWITCH_WRITE_DEFAULT = "1970-01-01T00:00:00Z;aruba";
    private final String TEMPORARY_EXCEPTION = "test temporary exception";
    private final String PERMANENT_EXCEPTION = "test permanent exception";
    private final String MESSAGE = "test message";
    private final String ARUBA_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@pec.aruba.it";
    private final String NAMIRIAL_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@sicurezzapostale.it";

    private final String DATE_R_ARUBA_W_ARUBA = "2022-12-02T00:00:00Z";
    private final String DATE_R_ARUBA_NAM_W_NAM = "2023-01-02T23:59:58Z";
    private final String DATE_R_NAM_W_ARUBA = "2023-02-02T00:00:00Z";
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
        void sendMailArubaOk() {
            log.debug("sendMailArubaPermanentException");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_NAM_W_ARUBA).getMillis());

            when(arubaService.sendMail(any())).thenReturn(Mono.just(MESSAGE));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectNext(MESSAGE).verifyComplete();

            verify(arubaService, times(1)).sendMail(any());
            verify(namirialService, never()).sendMail(any());
        }

        @Test
        void sendMailNamirialOk() {
            log.debug("sendMailArubaPermanentException");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());

            when(namirialService.sendMail(any())).thenReturn(Mono.just(MESSAGE));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectNext(MESSAGE).verifyComplete();

            verify(namirialService, times(1)).sendMail(any());
            verify(arubaService, never()).sendMail(any());
        }

        @Test
        void sendMailArubaPermanentException() {
            log.debug("sendMailArubaPermanentException");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_NAM_W_ARUBA).getMillis());

            when(arubaService.sendMail(any())).thenReturn(Mono.error(permanentException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).sendMail(any());
            verify(namirialService, never()).sendMail(any());
        }

        @Test
        void sendMailNamirialServicePermanentException() {
            log.debug("sendMailNamirialServicePermanentException");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());

            when(namirialService.sendMail(any())).thenReturn(Mono.error(permanentException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, never()).sendMail(any());
            verify(namirialService, times(1)).sendMail(any());
        }

        @Test
        void sendMailArubaRetriesExceeded() {
            log.debug("sendMailArubaRetriesExceeded");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_W_ARUBA).getMillis());

            when(arubaService.sendMail(any())).thenReturn(Mono.error(temporaryException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(ArubaCallMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).sendMail(any());
            verify(namirialService, never()).sendMail(any());
        }

        @Test
        void sendMailNamirialServiceRetriesExceeded() {
            log.debug("sendMailArubaRetriesExceeded");

            byte[] message = MESSAGE.getBytes();
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());

            when(namirialService.sendMail(any())).thenReturn(Mono.error(temporaryException));

            Mono<String> response = pnPecService.sendMail(message);
            StepVerifier.create(response).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, never()).sendMail(any());
            verify(namirialService, times(1)).sendMail(any());
        }
    }

    @Nested
    class getUnreadMessagesTests {

        @BeforeEach
        void setUp() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());
        }

        @AfterAll
        static void afterAll() {
            DateTimeUtils.setCurrentMillisSystem();
        }

        @Test
        void getUnreadMessagesExpectBothOk() {
            log.debug("getUnreadMessagesExpectBothOk");

            PnGetMessagesResponse arubaMessages = getArubaMessage();
            PnGetMessagesResponse namirialProviderMessages = getNamirialProviderMessages();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.just(namirialProviderMessages));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages).expectNextMatches(response ->
                            response.getNumOfMessages() == arubaMessages.getNumOfMessages() + namirialProviderMessages.getNumOfMessages()
                                    && response.getPnEcPecListOfMessages().getMessages().size() == arubaMessages.getPnListOfMessages().getMessages().size() + namirialProviderMessages.getPnListOfMessages().getMessages().size())
                    .expectComplete()
                    .verify();
            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, times(1)).getUnreadMessages(6);

        }

        @Test
        void getUnreadMessagesNamirialProviderKo() {
            log.debug("getUnreadMessagesNamirialProviderKo");

            PnGetMessagesResponse arubaMessages = getArubaMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(PnSpapiPermanentErrorException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesArubaKo() {
            log.debug("getUnreadMessagesArubaKo");
            PnGetMessagesResponse namirialProviderMessages = getNamirialProviderMessages();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.just(namirialProviderMessages));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(PnSpapiPermanentErrorException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, never()).getUnreadMessages(anyInt());
        }

        @Test
        void getUnreadMessagesBothKo() {
            log.debug("getUnreadMessagesBothKo");

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.error(permanentException));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(PnSpapiPermanentErrorException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, never()).getUnreadMessages(anyInt());
        }

        @Test
        void getUnreadMesagesBothEmpty() {
            log.debug("getUnreadMessagesBothEmpty");
            PnGetMessagesResponse arubaMessages = new PnGetMessagesResponse();
            PnGetMessagesResponse namirialProviderMessages = new PnGetMessagesResponse();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.just(namirialProviderMessages));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectNextMatches(response -> response.getNumOfMessages() == 0
                            && response.getPnEcPecListOfMessages() == null)
                    .expectComplete()
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesNamirialProviderRetriesExceeded() {
            log.debug("getUnreadMessagesNamirialProviderRetriesExceeded");
            PnGetMessagesResponse arubaMessages = getArubaMessage();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(NamirialProviderMaxRetriesExceededException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, times(1)).getUnreadMessages(6);
        }

        @Test
        void getUnreadMessagesArubaRetriesExceeded() {
            log.debug("getUnreadMessagesArubaRetriesExceeded");
            PnGetMessagesResponse namirialProviderMessages = getNamirialProviderMessages();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.just(namirialProviderMessages));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(ArubaCallMaxRetriesExceededException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, never()).getUnreadMessages(anyInt());
        }

        @Test
        void getUnreadMessagesBothRetriesExceeded() {
            log.debug("getUnreadMessagesNamirialProviderKo");

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.error(temporaryException));

            Mono<PnEcPecGetMessagesResponse> combinedMessages = pnPecService.getUnreadMessages(6);

            StepVerifier.create(combinedMessages)
                    .expectError(ArubaCallMaxRetriesExceededException.class)
                    .verify();

            verify(arubaService, times(1)).getUnreadMessages(6);
            verify(namirialService, never()).getUnreadMessages(anyInt());
        }
    }

    @Nested
    class getMessageCountTests {
        @BeforeEach
        void setUp() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());
        }

        @AfterAll
        static void afterAll() {
            DateTimeUtils.setCurrentMillisSystem();
        }

        @Test
        void getMessageCountExpectBothOk() {
            log.debug("getUnreadMessagesExpectBoth");

            when(arubaService.getMessageCount()).thenReturn(Mono.just(3));
            when(namirialService.getMessageCount()).thenReturn(Mono.just(3));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectNext(6).expectComplete().verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(1)).getMessageCount();
            verify(cloudWatchPecMetrics, times(1)).publishMessageCount(3L, arubaProviderNamespace);
            verify(cloudWatchPecMetrics, times(1)).publishMessageCount(3L, namirialProviderNamespace);
        }

        @Test
        void getMessageCountNamirialProviderKo() {
            log.debug("getUnreadMessagesNamirialProviderKo");

            when(arubaService.getMessageCount()).thenReturn(Mono.just(3));
            when(namirialService.getMessageCount()).thenReturn(Mono.error(permanentException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(1)).getMessageCount();
            verify(cloudWatchPecMetrics, times(1)).publishMessageCount(3L, arubaProviderNamespace);
            verify(cloudWatchPecMetrics, never()).publishMessageCount(anyLong(), eq(namirialProviderNamespace));
        }

        @Test
        void getMessageCountArubaKo() {
            log.debug("getUnreadMessagesArubaKo");

            when(arubaService.getMessageCount()).thenReturn(Mono.error(permanentException));
            when(namirialService.getMessageCount()).thenReturn(Mono.just(3));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(0)).getMessageCount();
            verify(cloudWatchPecMetrics, never()).publishMessageCount(anyLong(), anyString());
        }

        @Test
        void getMessageCountBothKo() {
            log.debug("getUnreadMessagesBothKo");

            when(arubaService.getMessageCount()).thenReturn(Mono.error(permanentException));
            when(namirialService.getMessageCount()).thenReturn(Mono.error(permanentException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(0)).getMessageCount();
            verify(cloudWatchPecMetrics, never()).publishMessageCount(anyLong(), anyString());
        }

        @Test
        void getMessageCountArubaRetriesExceeded() {
            log.debug("getUnreadMessagesArubaRetriesExceeded");

            when(arubaService.getMessageCount()).thenReturn(Mono.error(temporaryException));
            when(namirialService.getMessageCount()).thenReturn(Mono.just(3));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(ArubaCallMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(0)).getMessageCount();
            verify(cloudWatchPecMetrics, never()).publishMessageCount(anyLong(), anyString());
        }

        @Test
        void getMessageCountNamirialProviderRetriesExceeded() {
            log.debug("getUnreadMessagesNamirialProviderRetriesExceeded");

            when(arubaService.getMessageCount()).thenReturn(Mono.just(3));
            when(namirialService.getMessageCount()).thenReturn(Mono.error(temporaryException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount).expectError(NamirialProviderMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(1)).getMessageCount();
            verify(cloudWatchPecMetrics, times(1)).publishMessageCount(3L, arubaProviderNamespace);
            verify(cloudWatchPecMetrics, never()).publishMessageCount(anyLong(), eq(namirialProviderNamespace));
        }

        @Test
        void getMessageCountBothRetriesExceeded() {
            log.debug("getUnreadMessagesBothRetry");

            when(arubaService.getMessageCount()).thenReturn(Mono.error(temporaryException));
            when(namirialService.getMessageCount()).thenReturn(Mono.error(temporaryException));

            Mono<Integer> messageCount = pnPecService.getMessageCount();

            StepVerifier.create(messageCount)
                    .expectError(ArubaCallMaxRetriesExceededException.class)
                    .verify();

            verify(arubaService, times(1)).getMessageCount();
            verify(namirialService, times(0)).getMessageCount();
            verify(cloudWatchPecMetrics, never()).publishMessageCount(anyLong(), anyString());
        }
    }

    @Nested
    class MarkMessageAsReadTests {
        @Test
        void markMessageAsReadFromArubaOk() {
            log.debug("markMessageAsReadFromArubaOk");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());
            when(namirialService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID, ARUBA_PROVIDER)).expectComplete().verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).markMessageAsRead(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(markMessageAsReadResponseTimeMetric), anyLong());
        }

        @Test
        void markMessageAsReadFromNamirialOk() {
            log.debug("markMessageAsReadFromNamirialOk");

            when(arubaService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(namirialService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.markMessageAsRead(NAMIRIAL_MESSAGE_ID, NAMIRIAL_PROVIDER)).expectComplete().verify();

            verify(arubaService, never()).markMessageAsRead(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).markMessageAsRead(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(markMessageAsReadResponseTimeMetric), anyLong());
        }

        @Test
        void markMessageAsReadFromArubaKo() {
            log.debug("markMessageAsReadFromArubaKo");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(namirialService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID, ARUBA_PROVIDER)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromNamirialKo() {
            log.debug("markMessageAsReadFromNamirialKo");

            when(arubaService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(namirialService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(NAMIRIAL_MESSAGE_ID, NAMIRIAL_PROVIDER)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, never()).markMessageAsRead(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromArubaRetriesExceeded() {
            log.debug("markMessageAsReadFromArubaRetriesExceeded");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(namirialService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID, ARUBA_PROVIDER)).expectError(ArubaCallMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).markMessageAsRead(anyString());
        }

        @Test
        void markMessageAsReadFromNamirialRetriesExceeded() {
            log.debug("markMessageAsReadFromNamirialRetriesExceeded");

            when(arubaService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(namirialService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.markMessageAsRead(NAMIRIAL_MESSAGE_ID, NAMIRIAL_PROVIDER)).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, never()).markMessageAsRead(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).markMessageAsRead(anyString());
        }
    }

    @Nested
    class DeleteMessageTests {
        @Test
        void deleteMessageFromArubaOk() {
            log.debug("deleteMessageFromArubaOk");

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());
            when(namirialService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID, ARUBA_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).deleteMessage(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(deleteMessageResponseTimeMetric), anyLong());
        }

        @Test
        void deleteMessageFromNamirialOk() {
            log.debug("deleteMessageFromNamirialOk");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());

            when(arubaService.deleteMessage(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.empty());
            when(namirialService.deleteMessage(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.deleteMessage(NAMIRIAL_MESSAGE_ID, NAMIRIAL_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, never()).deleteMessage(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).deleteMessage(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(deleteMessageResponseTimeMetric), anyLong());
        }

        @Test
        void deleteMessageFromArubaKo() {
            log.debug("deleteMessageFromArubaKo");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_NAM_W_ARUBA).getMillis());

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(namirialService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID, ARUBA_MESSAGE_ID)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromNamirialKo() {
            log.debug("deleteMessageFromNamirialKo");

            when(arubaService.deleteMessage(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(namirialService.deleteMessage(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(permanentException));


            StepVerifier.create(pnPecService.deleteMessage(NAMIRIAL_MESSAGE_ID, NAMIRIAL_MESSAGE_ID)).expectError(PnSpapiPermanentErrorException.class).verify();

            verify(arubaService, never()).deleteMessage(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromArubaRetriesExceeded() {
            log.debug("deleteMessageFromArubaRetriesExceeded");

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));
            when(namirialService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID, ARUBA_MESSAGE_ID)).expectError(ArubaCallMaxRetriesExceededException.class).verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).deleteMessage(anyString());
        }

        @Test
        void deleteMessageFromNamirialRetriesExceeded() {
            log.debug("deleteMessageFromNamirialRetriesExceeded");

            when(arubaService.deleteMessage(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(namirialService.deleteMessage(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(temporaryException));

            StepVerifier.create(pnPecService.deleteMessage(NAMIRIAL_MESSAGE_ID, NAMIRIAL_MESSAGE_ID)).expectError(MaxRetriesExceededException.class).verify();

            verify(arubaService, never()).deleteMessage(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).deleteMessage(anyString());
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
        messageIDs.add(NAMIRIAL_MESSAGE_ID);

        Assertions.assertTrue(PnEcPecServiceImpl.isAruba(messageIDs.get(0)));
        Assertions.assertTrue(PnEcPecServiceImpl.isAruba(messageIDs.get(1)));
        Assertions.assertTrue(PnEcPecServiceImpl.isAruba(messageIDs.get(2)));
        Assertions.assertFalse(PnEcPecServiceImpl.isAruba(messageIDs.get(3)));
        Assertions.assertFalse(PnEcPecServiceImpl.isAruba(messageIDs.get(4)));

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

    private PnGetMessagesResponse getNamirialProviderMessages() {
        List<byte[]> namirialProviderMessages = new ArrayList<>();
        namirialProviderMessages.add("message1".getBytes());
        namirialProviderMessages.add("message2".getBytes());
        namirialProviderMessages.add("message3".getBytes());

        PnGetMessagesResponse namirialProviderMessagesResponse = new PnGetMessagesResponse();
        namirialProviderMessagesResponse.setPnListOfMessages(new PnListOfMessages(namirialProviderMessages));
        namirialProviderMessagesResponse.setNumOfMessages(3);
        return namirialProviderMessagesResponse;
    }

}