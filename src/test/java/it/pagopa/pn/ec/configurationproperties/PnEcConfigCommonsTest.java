package it.pagopa.pn.ec.configurationproperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PnEcConfig.class)
@EnableConfigurationProperties(PnEcConfig.class)
@TestPropertySource(properties = {
        "pn.ec.commons.transaction-process.start-status=BOOKED",
        "pn.ec.commons.transaction-process.sms=SMS",
        "pn.ec.commons.transaction-process.sms-start-status=BOOKED",
        "pn.ec.commons.transaction-process.email=EMAIL",
        "pn.ec.commons.transaction-process.email-start-status=BOOKED",
        "pn.ec.commons.transaction-process.pec=PEC",
        "pn.ec.commons.transaction-process.pec-start-status=BOOKED",
        "pn.ec.commons.transaction-process.paper=PAPER",
        "pn.ec.commons.transaction-process.paper-starter-status=BOOKED",
        "pn.ec.commons.transaction-process.sercq=SERCQ",
        "pn.ec.commons.transaction-process.sercq-start-status=BOOKED",

        "pn.ec.commons.shedlock.table-name=pn-EcShedlockCounter",
        "pn.ec.commons.shedlock.lock-at-most-for=PT5M",
        "pn.ec.commons.shedlock.lock-at-least-for=PT1M",
        "pn.ec.commons.shedlock.execute-batch-timeout-delta=PT10S",

        "pn.ec.commons.consolidatore.max-concurrent-requests=5",
        "pn.ec.commons.consolidatore.max-retry-for-rate-limiter=3",
        "pn.ec.commons.consolidatore.max-retry-for-rate-limiter-seconds=10",
        "pn.ec.commons.consolidatore.progresses-timeout-seconds=10",
        "pn.ec.commons.consolidatore.progresses-retry-after-seconds=30",
        "pn.ec.commons.consolidatore.rate-limiter.max-requests=2",
        "pn.ec.commons.consolidatore.rate-limiter.refresh-period-seconds=5",

        "pn.ec.commons.cloudwatch.maximum-calls-per-upload=20",
        "pn.ec.commons.cloudwatch.upload-frequency-millis=60000",
        "pn.ec.commons.cloudwatch.pec-metric-names.mark-message-as-read-response-time=markMessageAsReadResponseTime",
        "pn.ec.commons.cloudwatch.pec-metric-names.delete-message-response-time=deleteMessageResponseTime",
        "pn.ec.commons.cloudwatch.pec-metric-names.get-unread-messages-response-time=getUnreadMessagesResponseTime",
        "pn.ec.commons.cloudwatch.pec-metric-names.get-message-count-response-time=getMessageCountResponseTime",
        "pn.ec.commons.cloudwatch.pec-metric-names.send-mail-response-time=sendMailResponseTime",
        "pn.ec.commons.cloudwatch.pec-metric-names.payload-size-range=payloadSizeRange",
        "pn.ec.commons.cloudwatch.pec-metric-names.message-count-range=messageCountRange",
        "pn.ec.commons.cloudwatch.pec-metric-names.get-unread-pec-messages-count=getUnreadPecMessagesCount",

        "pn.ec.commons.endpoint.consolidatore.trust-all=true",
        "pn.ec.commons.endpoint.consolidatore.base-url=http://pn-consolidatore:8080",
        "pn.ec.commons.endpoint.consolidatore.base-path=/consolidatore",
        "pn.ec.commons.endpoint.consolidatore.client-header-name=X-Client",
        "pn.ec.commons.endpoint.consolidatore.client-header-value=pn-ec",
        "pn.ec.commons.endpoint.consolidatore.api-key-header-name=X-Api-Key",
        "pn.ec.commons.endpoint.consolidatore.api-key-header-value=api-key",
        "pn.ec.commons.endpoint.consolidatore.paper-messages.put-request=/put",
        "pn.ec.commons.endpoint.consolidatore.paper-messages.put-duplicate-request=/put-duplicate",
        "pn.ec.commons.endpoint.consolidatore.paper-messages.get-request-progress=/get",
        "pn.ec.commons.endpoint.consolidatore.paper-messages.get-duplicate-request-progress=/get-duplicate",

        "pn.ec.commons.endpoint.external-channel.container-base-url=http://pn-external-channel:8080",

        "pn.ec.commons.endpoint.gestore-repository.get-client-configuration=/getClientConfiguration",
        "pn.ec.commons.endpoint.gestore-repository.post-client-configuration=/postClientConfiguration",
        "pn.ec.commons.endpoint.gestore-repository.put-client-configuration=/putClientConfiguration",
        "pn.ec.commons.endpoint.gestore-repository.delete-client-configuration=/deleteClientConfiguration",
        "pn.ec.commons.endpoint.gestore-repository.get-request=/getRequest",
        "pn.ec.commons.endpoint.gestore-repository.post-request=/postRequest",
        "pn.ec.commons.endpoint.gestore-repository.patch-request=/patchRequest",
        "pn.ec.commons.endpoint.gestore-repository.delete-request=/deleteRequest",
        "pn.ec.commons.endpoint.gestore-repository.get-request-by-message-id=/getRequestByMessageId",
        "pn.ec.commons.endpoint.gestore-repository.post-discarded-events=/postDiscardedEvents",
        "pn.ec.commons.endpoint.gestore-repository.set-message-id-in-request-metadata=/setMessageIdInRequestMetadata",
        "pn.ec.commons.endpoint.gestore-repository.get-request-metadata-by-message-id=/getRequestMetadataByMessageId",
        "pn.ec.commons.endpoint.gestore-repository.set-request-metadata-message-id=/setRequestMetadataMessageId",

        "pn.ec.commons.endpoint.files.get-file=/getFile",
        "pn.ec.commons.endpoint.files.post-file=/postFile",

        "pn.ec.commons.endpoint.safe-storage.container-base-url=http://pn-safe-storage:8080",
        "pn.ec.commons.endpoint.safe-storage.client-header-name=X-Client",
        "pn.ec.commons.endpoint.safe-storage.client-header-value=pn-ec",
        "pn.ec.commons.endpoint.safe-storage.api-key-header-name=X-Api-Key",
        "pn.ec.commons.endpoint.safe-storage.api-key-header-value=api-key",
        "pn.ec.commons.endpoint.safe-storage.checksum-value-header-name=X-Checksum",
        "pn.ec.commons.endpoint.safe-storage.trace-id-header-name=X-TraceId",

        "pn.ec.commons.ricezione-esiti-cartaceo.consider-event-without-sent-status-as-booked=true",
        "pn.ec.commons.ricezione-esiti-cartaceo.duplicates-check=productType1:NONBLOCKING;productType2",
        "pn.ec.commons.ricezione-esiti-cartaceo.allowed-future-offset-duration=PT1M",
        "pn.ec.commons.ricezione-esiti-cartaceo.duplicated-event-error-code=400.02"
})
class PnEcConfigCommonsTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindTransactionProcessProperties() {
        var transactionProcess = pnEcConfig.getCommons().getTransactionProcess();
        assertThat(transactionProcess.getStartStatus()).isEqualTo("BOOKED");
        assertThat(transactionProcess.getSms()).isEqualTo("SMS");
        assertThat(transactionProcess.getSmsStartStatus()).isEqualTo("BOOKED");
        assertThat(transactionProcess.getEmail()).isEqualTo("EMAIL");
        assertThat(transactionProcess.getEmailStartStatus()).isEqualTo("BOOKED");
        assertThat(transactionProcess.getPec()).isEqualTo("PEC");
        assertThat(transactionProcess.getPecStartStatus()).isEqualTo("BOOKED");
        assertThat(transactionProcess.getPaper()).isEqualTo("PAPER");
        assertThat(transactionProcess.getPaperStarterStatus()).isEqualTo("BOOKED");
        assertThat(transactionProcess.getSercq()).isEqualTo("SERCQ");
        assertThat(transactionProcess.getSercqStartStatus()).isEqualTo("BOOKED");
    }

    @Test
    void shouldBindShedlockProperties() {
        var shedlock = pnEcConfig.getCommons().getShedlock();
        assertThat(shedlock.getTableName()).isEqualTo("pn-EcShedlockCounter");
        assertThat(shedlock.getLockAtMostFor()).isEqualTo("PT5M");
        assertThat(shedlock.getLockAtLeastFor()).isEqualTo("PT1M");
        assertThat(shedlock.getExecuteBatchTimeoutDelta()).isEqualTo("PT10S");
    }

    @Test
    void shouldBindConsolidatoreProperties() {
        var consolidatore = pnEcConfig.getCommons().getConsolidatore();
        assertThat(consolidatore.getMaxConcurrentRequests()).isEqualTo(5);
        assertThat(consolidatore.getMaxRetryForRateLimiter()).isEqualTo(3);
        assertThat(consolidatore.getMaxRetryForRateLimiterSeconds()).isEqualTo(10);
        assertThat(consolidatore.getProgressesTimeoutSeconds()).isEqualTo(10);
        assertThat(consolidatore.getProgressesRetryAfterSeconds()).isEqualTo(30);
        assertThat(consolidatore.getRateLimiter().getMaxRequests()).isEqualTo(2);
        assertThat(consolidatore.getRateLimiter().getRefreshPeriodSeconds()).isEqualTo(5);
    }

    @Test
    void shouldBindCloudwatchProperties() {
        var cloudwatch = pnEcConfig.getCommons().getCloudWatch();
        assertThat(cloudwatch.getMaximumCallsPerUpload()).isEqualTo(20);
        assertThat(cloudwatch.getUploadFrequencyMillis()).isEqualTo(60000L);
        var pecMetricNames = cloudwatch.getPecMetricNames();
        assertThat(pecMetricNames.getMarkMessageAsReadResponseTime()).isEqualTo("markMessageAsReadResponseTime");
        assertThat(pecMetricNames.getDeleteMessageResponseTime()).isEqualTo("deleteMessageResponseTime");
        assertThat(pecMetricNames.getGetUnreadMessagesResponseTime()).isEqualTo("getUnreadMessagesResponseTime");
        assertThat(pecMetricNames.getGetMessageCountResponseTime()).isEqualTo("getMessageCountResponseTime");
        assertThat(pecMetricNames.getSendMailResponseTime()).isEqualTo("sendMailResponseTime");
        assertThat(pecMetricNames.getPayloadSizeRange()).isEqualTo("payloadSizeRange");
        assertThat(pecMetricNames.getMessageCountRange()).isEqualTo("messageCountRange");
        assertThat(pecMetricNames.getGetUnreadPecMessagesCount()).isEqualTo("getUnreadPecMessagesCount");
    }

    @Test
    void shouldBindConsolidatoreEndpointProperties() {
        var consolidatoreEndpoint = pnEcConfig.getCommons().getEndpoint().getConsolidatore();
        assertThat(consolidatoreEndpoint.getTrustAll()).isTrue();
        assertThat(consolidatoreEndpoint.getBaseUrl()).isEqualTo("http://pn-consolidatore:8080");
        assertThat(consolidatoreEndpoint.getBasePath()).isEqualTo("/consolidatore");
        assertThat(consolidatoreEndpoint.getClientHeaderName()).isEqualTo("X-Client");
        assertThat(consolidatoreEndpoint.getClientHeaderValue()).isEqualTo("pn-ec");
        assertThat(consolidatoreEndpoint.getApiKeyHeaderName()).isEqualTo("X-Api-Key");
        assertThat(consolidatoreEndpoint.getApiKeyHeaderValue()).isEqualTo("api-key");
        var paperMessages = consolidatoreEndpoint.getPaperMessages();
        assertThat(paperMessages.getPutRequest()).isEqualTo("/put");
        assertThat(paperMessages.getPutDuplicateRequest()).isEqualTo("/put-duplicate");
        assertThat(paperMessages.getGetRequestProgress()).isEqualTo("/get");
        assertThat(paperMessages.getGetDuplicateRequestProgress()).isEqualTo("/get-duplicate");
    }

    @Test
    void shouldBindExternalChannelEndpointProperties() {
        assertThat(pnEcConfig.getCommons().getEndpoint().getExternalChannel().getContainerBaseUrl())
                .isEqualTo("http://pn-external-channel:8080");
    }

    @Test
    void shouldBindGestoreRepositoryEndpointProperties() {
        var gestoreRepository = pnEcConfig.getCommons().getEndpoint().getGestoreRepository();
        assertThat(gestoreRepository.getGetClientConfiguration()).isEqualTo("/getClientConfiguration");
        assertThat(gestoreRepository.getPostClientConfiguration()).isEqualTo("/postClientConfiguration");
        assertThat(gestoreRepository.getPutClientConfiguration()).isEqualTo("/putClientConfiguration");
        assertThat(gestoreRepository.getDeleteClientConfiguration()).isEqualTo("/deleteClientConfiguration");
        assertThat(gestoreRepository.getGetRequest()).isEqualTo("/getRequest");
        assertThat(gestoreRepository.getPostRequest()).isEqualTo("/postRequest");
        assertThat(gestoreRepository.getPatchRequest()).isEqualTo("/patchRequest");
        assertThat(gestoreRepository.getDeleteRequest()).isEqualTo("/deleteRequest");
        assertThat(gestoreRepository.getGetRequestByMessageId()).isEqualTo("/getRequestByMessageId");
        assertThat(gestoreRepository.getPostDiscardedEvents()).isEqualTo("/postDiscardedEvents");
        assertThat(gestoreRepository.getSetMessageIdInRequestMetadata()).isEqualTo("/setMessageIdInRequestMetadata");
        assertThat(gestoreRepository.getGetRequestMetadataByMessageId()).isEqualTo("/getRequestMetadataByMessageId");
        assertThat(gestoreRepository.getSetRequestMetadataMessageId()).isEqualTo("/setRequestMetadataMessageId");
    }

    @Test
    void shouldBindFilesEndpointProperties() {
        var files = pnEcConfig.getCommons().getEndpoint().getFiles();
        assertThat(files.getGetFile()).isEqualTo("/getFile");
        assertThat(files.getPostFile()).isEqualTo("/postFile");
    }

    @Test
    void shouldBindSafeStorageEndpointProperties() {
        var safeStorage = pnEcConfig.getCommons().getEndpoint().getSafeStorage();
        assertThat(safeStorage.getContainerBaseUrl()).isEqualTo("http://pn-safe-storage:8080");
        assertThat(safeStorage.getClientHeaderName()).isEqualTo("X-Client");
        assertThat(safeStorage.getClientHeaderValue()).isEqualTo("pn-ec");
        assertThat(safeStorage.getApiKeyHeaderName()).isEqualTo("X-Api-Key");
        assertThat(safeStorage.getApiKeyHeaderValue()).isEqualTo("api-key");
        assertThat(safeStorage.getChecksumValueHeaderName()).isEqualTo("X-Checksum");
        assertThat(safeStorage.getTraceIdHeaderName()).isEqualTo("X-TraceId");
    }

    @Test
    void shouldBindRicezioneEsitiCartaceoProperties() {
        var ricezioneEsitiCartaceo = pnEcConfig.getCommons().getRicezioneEsitiCartaceo();
        assertThat(ricezioneEsitiCartaceo.getConsiderEventWithoutSentStatusAsBooked()).isEqualTo("true");
        assertThat(ricezioneEsitiCartaceo.getDuplicatesCheck()).isEqualTo("productType1:NONBLOCKING;productType2");
        assertThat(ricezioneEsitiCartaceo.getAllowedFutureOffsetDuration()).isEqualTo("PT1M");
        assertThat(ricezioneEsitiCartaceo.getDuplicatedEventErrorCode()).isEqualTo("400.02");
    }
}
