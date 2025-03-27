package it.pagopa.pn.ec.pec.service.impl;


import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.configurationproperties.PnPecMetricNames;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.*;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pagopa.pn.library.pec.service.impl.PnEcPecServiceImpl;
import it.pagopa.pn.library.pec.utils.PnPecUtils;
import it.pagopa.pn.template.service.DummyPecService;
import lombok.CustomLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.pec.service.impl.PecServiceTest.createDigitalNotificationRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.*;
import static org.apache.commons.lang3.reflect.TypeUtils.isInstance;
import static org.assertj.core.api.Assertions.assertThat;
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
    @SpyBean
    private DummyPecService dummyPecService;
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
    @Autowired
    private PnPecMetricNames pnPecMetricNames;

    private String providerSwitchReadDefault = "1970-01-01T00:00:00Z;aruba";
    private String providerSwitchWriteDefault = "1970-01-01T00:00:00Z;aruba";
    private static final String PROVIDER_SWITCH_READ_DUMMY= "1970-01-01T00:00:00Z;dummy";
    private static final String PROVIDER_SWITCH_WRITE_READ_MULTIPLE_PROVIDERS= "1970-01-01T00:00:00Z;aruba|namirial|dummy";
    private static final String PROVIDER_SWITCH_WRITE_DUMMY = "1970-01-01T00:00:00Z;dummy";
    private static final String TEMPORARY_EXCEPTION = "test temporary exception";
    private static final String PERMANENT_EXCEPTION = "test permanent exception";
    private static final String MESSAGE = "test message";
    private static final String ARUBA_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@pec.aruba.it";
    private static final String NAMIRIAL_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@sicurezzapostale.it";
    private static final String DUMMY_MESSAGE_ID = "opec21010.20231006185001.00057.206.1.59@pec.dummy.it";

    private static final String DATE_R_ARUBA_W_ARUBA = "2022-12-02T00:00:00Z";
    private static final String DATE_R_ARUBA_NAM_W_NAM = "2023-01-02T23:59:58Z";
    private static final String DATE_R_NAM_W_ARUBA = "2023-02-02T00:00:00Z";
    private static final String DATE_DEFAULT = "1970-01-01T00:00:00Z";


    private final PnSpapiPermanentErrorException permanentException = new PnSpapiPermanentErrorException(PERMANENT_EXCEPTION);
    private final PnSpapiTemporaryErrorException temporaryException = new PnSpapiTemporaryErrorException(TEMPORARY_EXCEPTION);
    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO = PecPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX).xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE).digitalNotificationRequest(createDigitalNotificationRequest()).build();

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).getMillis());
        providerSwitchReadDefault = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "pnPecProviderSwitchRead");
        providerSwitchWriteDefault = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite");
    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchRead", providerSwitchReadDefault);
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", providerSwitchWriteDefault);
    }

    @AfterAll
    static void afterAll() {
        DateTimeUtils.setCurrentMillisSystem();
    }


    @Nested
    class sendMailTests {

        @Test
        void sendMailWithDummy(){
            log.debug("sendMailWithDummy");

            setDummyProviderSwitch();

            when(dummyPecService.sendMail(any())).thenReturn(Mono.just(MESSAGE));

            byte[] message = MESSAGE.getBytes();

            StepVerifier.create(pnPecService.sendMail(message)).expectNext(MESSAGE).expectComplete().verify();

            verify(arubaService, never()).sendMail(any());
            verify(namirialService, never()).sendMail(any());
            verify(dummyPecService, times(1)).sendMail(any());
        }

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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getSendMailResponseTime()), anyLong(), argThat(list -> list.size() == 1));
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(pnPecMetricNames.getSendMailResponseTime()), anyLong(), argThat(list -> list.size() == 1));
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
        void getUnreadMessagesWithDummy(){
            log.debug("getUnreadMessagesWithDummy");

            setDummyProviderSwitch();

            StepVerifier.create(pnPecService.getUnreadMessages(6)).expectNextMatches(response -> response.getNumOfMessages() == 0 && response.getPnEcPecListOfMessages() == null).expectComplete().verify();

            verify(arubaService, never()).getUnreadMessages(anyInt());
            verify(namirialService, never()).getUnreadMessages(anyInt());
            verify(dummyPecService, times(1)).getUnreadMessages(anyInt());
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetUnreadMessagesResponseTime()), anyLong(), argThat(list -> list.size() == 1));
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(pnPecMetricNames.getGetUnreadMessagesResponseTime()), anyLong(), argThat(list -> list.size() == 1));
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetUnreadMessagesResponseTime()), anyLong(), argThat(list -> list.size() == 1));
            verify(cloudWatchPecMetrics, never()).publishResponseTime(eq(namirialProviderNamespace), any(), anyLong(), any());
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), any());
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), any());
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetUnreadMessagesResponseTime()), anyLong(), argThat(list -> list.size() == 1));
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(pnPecMetricNames.getGetUnreadMessagesResponseTime()), anyLong(), argThat(list -> list.size() == 1));
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetUnreadMessagesResponseTime()), anyLong(), argThat(list -> list.size() == 1));
            verify(cloudWatchPecMetrics, never()).publishResponseTime(eq(namirialProviderNamespace), any(), anyLong(), any());
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), any());
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), any());
        }

        @Test
        void getUnreadMessagesPECAndPublishMetricsOk() {
            PnGetMessagesResponse arubaMessages = getArubaMessage();
            PnGetMessagesResponse namirialMessages = getNamirialProviderMessages();

            when(arubaService.getUnreadMessages(anyInt())).thenReturn(Mono.just(arubaMessages));
            when(namirialService.getUnreadMessages(anyInt())).thenReturn(Mono.just(namirialMessages));

            try (MockedStatic<PnPecUtils> mockedPecUtils = mockStatic(PnPecUtils.class)) {
                mockedPecUtils.when(() -> PnPecUtils.createEmfJson(any(), any(), anyLong()))
                        .thenReturn("MOCKED_JSON_LOG");


                Mono<PnEcPecGetMessagesResponse> result = pnPecService.getUnreadMessages(6);

                StepVerifier.create(result)
                        .expectNextMatches(response ->
                                response.getNumOfMessages() == arubaMessages.getNumOfMessages() + namirialMessages.getNumOfMessages()
                                        && response.getPnEcPecListOfMessages().getMessages().size() ==
                                        arubaMessages.getPnListOfMessages().getMessages().size() + namirialMessages.getPnListOfMessages().getMessages().size())
                        .verifyComplete();

            }
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
        void getMessageCountWithDummy(){
            log.debug("getMessageCountWithDummy");

            setDummyProviderSwitch();

            StepVerifier.create(pnPecService.getMessageCount()).expectNext(0).expectComplete().verify();

            verify(arubaService, never()).getMessageCount();
            verify(namirialService, never()).getMessageCount();
            verify(dummyPecService, times(1)).getMessageCount();
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetMessageCountResponseTime()), anyLong(), argThat(list -> list.size() == 0));
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(pnPecMetricNames.getGetMessageCountResponseTime()), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetMessageCountResponseTime()), anyLong(), argThat(list -> list.size() == 0));
            verify(cloudWatchPecMetrics, never()).publishResponseTime(eq(namirialProviderNamespace), any(), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getGetMessageCountResponseTime()), anyLong(), argThat(list -> list.size() == 0));
            verify(cloudWatchPecMetrics, never()).publishResponseTime(eq(namirialProviderNamespace), any(), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, never()).publishResponseTime(any(), any(), anyLong(), argThat(list -> list.size() == 0));
        }
    }

    @Nested
    class MarkMessageAsReadTests {

        @Test
        void markMessageAsReadWithDummy(){
            log.debug("markMessageAsReadWithDummy");

            setDummyProviderSwitch();
            when(dummyPecService.markMessageAsRead(DUMMY_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.markMessageAsRead(DUMMY_MESSAGE_ID, DUMMY_PROVIDER)).expectComplete().verify();

            verify(arubaService, never()).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).markMessageAsRead(anyString());
            verify(dummyPecService, times(1)).markMessageAsRead(anyString());
        }
        @Test
        void markMessageAsReadFromArubaOk() {
            log.debug("markMessageAsReadFromArubaOk");

            when(arubaService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());
            when(namirialService.markMessageAsRead(ARUBA_MESSAGE_ID)).thenReturn(Mono.error(permanentException));

            StepVerifier.create(pnPecService.markMessageAsRead(ARUBA_MESSAGE_ID, ARUBA_PROVIDER)).expectComplete().verify();

            verify(arubaService, times(1)).markMessageAsRead(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).markMessageAsRead(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getMarkMessageAsReadResponseTime()), anyLong(), argThat(list -> list.size() == 0));
        }

        @Test
        void markMessageAsReadFromNamirialOk() {
            log.debug("markMessageAsReadFromNamirialOk");

            when(arubaService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.error(permanentException));
            when(namirialService.markMessageAsRead(NAMIRIAL_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.markMessageAsRead(NAMIRIAL_MESSAGE_ID, NAMIRIAL_PROVIDER)).expectComplete().verify();

            verify(arubaService, never()).markMessageAsRead(NAMIRIAL_MESSAGE_ID);
            verify(namirialService, times(1)).markMessageAsRead(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(pnPecMetricNames.getMarkMessageAsReadResponseTime()), anyLong(), argThat(list -> list.size() == 0));
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
        void deleteMessageWithDummy(){
            log.debug("deleteMessageFromDummy");

            setDummyProviderSwitch();
            when(dummyPecService.deleteMessage(DUMMY_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.deleteMessage(DUMMY_MESSAGE_ID, DUMMY_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, never()).deleteMessage(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).deleteMessage(anyString());
            verify(dummyPecService, times(1)).deleteMessage(anyString());
        }
        @Test
        void deleteMessageFromArubaOk() {
            log.debug("deleteMessageFromArubaOk");

            when(arubaService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());
            when(namirialService.deleteMessage(ARUBA_MESSAGE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(pnPecService.deleteMessage(ARUBA_MESSAGE_ID, ARUBA_MESSAGE_ID)).expectComplete().verify();

            verify(arubaService, times(1)).deleteMessage(ARUBA_MESSAGE_ID);
            verify(namirialService, never()).deleteMessage(anyString());
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(arubaProviderNamespace), eq(pnPecMetricNames.getDeleteMessageResponseTime()), anyLong(), argThat(list -> list.size() == 0));
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
            verify(cloudWatchPecMetrics, times(1)).publishResponseTime(eq(namirialProviderNamespace), eq(pnPecMetricNames.getDeleteMessageResponseTime()), anyLong(), argThat(list -> list.size() == 0));
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

    @Nested
    class DummyTests{
        @Test
        void getProviderWriteTest(){
            log.debug("getProviderWriteTest");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).plusMillis(1).getMillis());
            ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", PROVIDER_SWITCH_WRITE_DUMMY);
            PnPecService provider = ReflectionTestUtils.invokeMethod(pnPecService, "getProviderWrite");
            Assertions.assertTrue(isInstance(provider, DummyPecService.class));
        }

        @Test
        void getProviderReadSingleValueTest(){
            log.debug("getProviderReadSingleValueTest");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).plusMillis(1).getMillis());
            ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchRead", PROVIDER_SWITCH_READ_DUMMY);
            List<String> providers = ReflectionTestUtils.invokeMethod(pnPecService, "getProvidersRead");
            Assertions.assertEquals(1, providers.size());
            Assertions.assertTrue(isInstance(providers.get(0), DummyPecService.class));
        }

        @Test
        void getProviderReadMultipleValuesTest(){
            log.debug("getProviderReadMultipleValuesTest");
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).plusMillis(1).getMillis());
            ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchRead", PROVIDER_SWITCH_WRITE_READ_MULTIPLE_PROVIDERS);
            List<String> providers = ReflectionTestUtils.invokeMethod(pnPecService, "getProvidersRead");
            Assertions.assertEquals(1, providers.size());
            Assertions.assertTrue(isInstance(providers.get(0), DummyPecService.class));
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
    private void setDummyProviderSwitch(){
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).plusMillis(1).getMillis());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchRead", PROVIDER_SWITCH_WRITE_READ_MULTIPLE_PROVIDERS);
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", PROVIDER_SWITCH_WRITE_DUMMY);
    }
}