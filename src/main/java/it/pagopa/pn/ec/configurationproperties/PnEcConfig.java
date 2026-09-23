package it.pagopa.pn.ec.configurationproperties;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "pn.ec")
@Import(SharedAutoConfiguration.class)
@Getter
@Setter
@CustomLog
public class PnEcConfig {

    private Cartaceo cartaceo = new Cartaceo();
    private Email email = new Email();
    private Pec pec = new Pec();
    private Sms sms = new Sms();
    private Sercq sercq = new Sercq();
    private Storage storage = new Storage();
    private Sqs sqs = new Sqs();
    private Dynamo dynamo = new Dynamo();
    private StateMachine stateMachine = new StateMachine();
    private NotificationTracker notificationTracker = new NotificationTracker();
    private ScaricamentoEsitiPec scaricamentoEsitiPec = new ScaricamentoEsitiPec();
    private CancellazioneRicevutePec cancellazioneRicevutePec = new CancellazioneRicevutePec();
    private PdfRaster pdfRaster = new PdfRaster();
    private Commons commons = new Commons();
    private AvailabilityManager availabilityManager = new AvailabilityManager();

    @PostConstruct
    public void logConfiguration() {
        log.info("PnEcConfig - cartaceo.sqsQueue.batchName={}", cartaceo.getSqsQueue().getBatchName());
        log.info("PnEcConfig - email.sqsQueue.batchName={}", email.getSqsQueue().getBatchName());
        log.info("PnEcConfig - pec.sqsQueue.batchName={}", pec.getSqsQueue().getBatchName());
        log.info("PnEcConfig - sms.sqsQueue.batchName={}", sms.getSqsQueue().getBatchName());
        log.info("PnEcConfig - sercq.receiverDigitalAddress={}", sercq.getReceiverDigitalAddress());
        log.info("PnEcConfig - storage.stagingBucket={}", storage.getStagingBucket());
        log.info("PnEcConfig - sqs.maxBatchSubscribedMsgs={}", sqs.getMaxBatchSubscribedMsgs());
        log.info("PnEcConfig - dynamo.repositoryManager.anagraficaClientName={}", dynamo.getRepositoryManager().getAnagraficaClientName());
        log.info("PnEcConfig - stateMachine.endpoint.containerBaseUrl={}", stateMachine.getEndpoint().getContainerBaseUrl());
        log.info("PnEcConfig - notificationTracker.eventBridge.notificationsBusName={}", notificationTracker.getEventBridge().getNotificationsBusName());
        log.info("PnEcConfig - scaricamentoEsitiPec.sqsQueueName={}", scaricamentoEsitiPec.getSqsQueueName());
        log.info("PnEcConfig - cancellazioneRicevutePec.sqsQueueName={}", cancellazioneRicevutePec.getSqsQueueName());
        log.info("PnEcConfig - pdfRaster.endpoint.baseUrl={}", pdfRaster.getEndpoint().getBaseUrl());
        log.info("PnEcConfig - commons.endpoint.externalChannel.containerBaseUrl={}", commons.getEndpoint().getExternalChannel().getContainerBaseUrl());
        log.info("PnEcConfig - availabilityManager.queueName={}", availabilityManager.getQueueName());
        log.info("PnEcConfig - commons.ricezioneEsitiCartaceo.duplicatedEventErrorCode={}", commons.getRicezioneEsitiCartaceo().getDuplicatedEventErrorCode());
    }

    @Getter
    @Setter
    public static class AvailabilityManager {
        private String queueName;
    }

    @Getter
    @Setter
    public static class Cartaceo {
        private SqsQueue sqsQueue = new SqsQueue();
        private Lavorazione lavorazione = new Lavorazione();
        private Paper paper = new Paper();
        private String esitiParameterName;

        @Getter
        @Setter
        public static class SqsQueue {
            private String batchName;
            private String errorName;
            private String dlqErrorName;
            private Integer maxBatchSubscribedMsgs;
        }

        @Getter
        @Setter
        public static class Lavorazione {
            private Integer maxThreadPoolSize;
            private Long maxRetryAttempts;
            private Long minRetryBackoff;
        }

        @Getter
        @Setter
        public static class Paper {
            private String documentTypeToTransform;
            private String documentTypeForRasterized;
            private String documentTypeForNormalized;
            private String paIdToRaster;
            private String paIdToNormalize;
            private String paIdOverride;
            private String transformationPriority;
        }
    }

    @Getter
    @Setter
    public static class Email {
        private String defaultSenderAddress;
        private Integer maxThreadPoolSize;
        private SqsQueue sqsQueue = new SqsQueue();
        private Ses ses = new Ses();

        @Getter
        @Setter
        public static class SqsQueue {
            private String batchName;
            private String interactiveName;
            private String errorName;
            private String sesEventsName;
        }

        @Getter
        @Setter
        public static class Ses {
            private String eventsListDefault;
        }
    }

    @Getter
    @Setter
    public static class Pec {
        private String attachmentRule;
        private Integer maxMessageSizeMb;
        private String tipoRicevutaHeaderName;
        private String tipoRicevutaHeaderValue;
        private Integer maxThreadPoolSize;
        private String identity;
        private String arubaServerAddress;
        private String dimensionMetricsSchema;
        private String namirialWarningToNotdeliveredLogic;
        private SqsQueue sqsQueue = new SqsQueue();
        private RetryStrategy retryStrategy = new RetryStrategy();
        private Postacert postacert = new Postacert();

        @Getter
        @Setter
        public static class SqsQueue {
            private String batchName;
            private String interactiveName;
            private String errorName;
        }

        @Getter
        @Setter
        public static class RetryStrategy {
            private String maxAttempts;
            private String minBackoff;
        }

        @Getter
        @Setter
        public static class Postacert {
            private String arubaPath;
            private String pnPath;
            private String namirialPath;
        }
    }

    @Getter
    @Setter
    public static class Sms {
        private Integer maxThreadPoolSize;
        private StressTest stressTest = new StressTest();
        private SqsQueue sqsQueue = new SqsQueue();
        private SnsTopic snsTopic = new SnsTopic();

        public Boolean getStressTestMode() {
            return stressTest.getMode();
        }

        public String getStressTestTopicArn() {
            return stressTest.getTopicArn();
        }

        @Getter
        @Setter
        public static class StressTest {
            private Boolean mode;
            private String topicArn;
        }

        @Getter
        @Setter
        public static class SqsQueue {
            private String batchName;
            private String interactiveName;
            private String errorName;
        }

        @Getter
        @Setter
        public static class SnsTopic {
            private String defaultSenderIdKey;
            private String defaultSenderIdValue;
            private String defaultSenderIdType;
        }
    }

    @Getter
    @Setter
    public static class Sercq {
        private String receiverDigitalAddress;
    }

    @Getter
    @Setter
    public static class Storage {
        private S3 s3 = new S3();
        private String stagingBucket;

        @Getter
        @Setter
        public static class S3 {
            private RetryStrategy retryStrategy = new RetryStrategy();

            @Getter
            @Setter
            public static class RetryStrategy {
                private Long maxAttempts;
                private Long minBackoff;
            }
        }
    }

    @Getter
    @Setter
    public static class Sqs {
        private RetryStrategy retryStrategy = new RetryStrategy();
        private Timeout timeout = new Timeout();
        private Integer maxBatchSubscribedMsgs;
        private Long maxMessageSize;

        @Getter
        @Setter
        public static class RetryStrategy {
            private Long maxAttempts;
            private Long minBackoff;
        }

        @Getter
        @Setter
        public static class Timeout {
            private Integer percent;
            private Long defaultSeconds;
            private List<String> managedQueues;
        }
    }

    @Getter
    @Setter
    public static class Dynamo {
        private RepositoryManager repositoryManager = new RepositoryManager();

        @Getter
        @Setter
        public static class RepositoryManager {
            private String anagraficaClientName;
            private String richiesteMetadataName;
            private String richiestePersonalName;
            private String richiesteConversioneRequestName;
            private String richiesteConversionePdfName;
            private String scartiConsolidatoreName;
        }
    }

    @Getter
    @Setter
    public static class StateMachine {
        private RetryStrategy retryStrategy = new RetryStrategy();
        private Endpoint endpoint = new Endpoint();

        @Getter
        @Setter
        public static class RetryStrategy {
            private Long maxAttempts;
            private Long minBackoff;
        }

        @Getter
        @Setter
        public static class Endpoint {
            private String containerBaseUrl;
            private String validate;
            private String decode;
        }
    }

    @Getter
    @Setter
    public static class NotificationTracker {
        private SqsQueue sqsQueue = new SqsQueue();
        private EventBridge eventBridge = new EventBridge();

        @Getter
        @Setter
        public static class SqsQueue {
            private String statoSmsName;
            private String statoSmsErratoName;
            private String statoSmsDlqName;
            private String statoEmailName;
            private String statoEmailErratoName;
            private String statoEmailDlqName;
            private String statoPecName;
            private String statoPecErratoName;
            private String statoPecDlqName;
            private String statoCartaceoName;
            private String statoCartaceoErratoName;
            private String statoCartaceoDlqName;
            private String statoSercqName;
            private String statoSercqErratoName;
            private Integer delaySeconds;
            private Long elapsedTimeSeconds;
        }

        @Getter
        @Setter
        public static class EventBridge {
            private String notificationsBusName;
        }
    }

    @Getter
    @Setter
    public static class ScaricamentoEsitiPec {
        private String getMessagesLimit;
        private String sqsQueueName;
        private String clientHeaderValue;
        private String apiKeyHeaderValue;
        private Integer limitRate;
        private String dumpEmail;
        private Lavorazione lavorazione = new Lavorazione();

        @Getter
        @Setter
        public static class Lavorazione {
            private Integer maxThreadPoolSize;
        }
    }

    @Getter
    @Setter
    public static class CancellazioneRicevutePec {
        private String sqsQueueName;
        private Integer maxThreadPoolSize;
    }

    @Getter
    @Setter
    public static class PdfRaster {
        private Long maxRetryAttempts;
        private Long minRetryBackoff;
        private Integer pdfConversionExpirationOffsetInDays;
        private Endpoint endpoint = new Endpoint();

        @Getter
        @Setter
        public static class Endpoint {
            private String baseUrl;
            private String basePath;
            private String convertPdf;
            private String clientHeaderValue;
            private String clientHeaderApiKey;
        }
    }

    @Getter
    @Setter
    public static class Commons {
        private TransactionProcess transactionProcess = new TransactionProcess();
        private Shedlock shedlock = new Shedlock();
        private Consolidatore consolidatore = new Consolidatore();
        private CloudWatch cloudWatch = new CloudWatch();
        private Endpoint endpoint = new Endpoint();
        private RicezioneEsitiCartaceo ricezioneEsitiCartaceo = new RicezioneEsitiCartaceo();

        @Getter
        @Setter
        public static class RicezioneEsitiCartaceo {
            private String considerEventWithoutSentStatusAsBooked;
            private String duplicatesCheck;
            private String allowedFutureOffsetDuration;
            private String duplicatedEventErrorCode;
        }

        @Getter
        @Setter
        public static class TransactionProcess {
            private String startStatus;
            private String sms;
            private String smsStartStatus;
            private String email;
            private String emailStartStatus;
            private String pec;
            private String pecStartStatus;
            private String paper;
            private String paperStarterStatus;
            private String sercq;
            private String sercqStartStatus;
        }

        @Getter
        @Setter
        public static class Shedlock {
            private String tableName;
            private String lockAtMostFor;
            private String lockAtLeastFor;
            private String executeBatchTimeoutDelta;
        }

        @Getter
        @Setter
        public static class Consolidatore {
            private Integer maxConcurrentRequests;
            private Integer maxRetryForRateLimiter;
            private Integer maxRetryForRateLimiterSeconds;
            private Integer progressesTimeoutSeconds;
            private Integer progressesRetryAfterSeconds;
            private RateLimiter rateLimiter = new RateLimiter();

            @Getter
            @Setter
            public static class RateLimiter {
                private Integer maxRequests;
                private Integer refreshPeriodSeconds;
            }
        }

        @Getter
        @Setter
        public static class CloudWatch {
            private Integer maximumCallsPerUpload;
            private Long uploadFrequencyMillis;
            private String pecNamespaceAruba;
            private String pecNamespaceNamirial;
            private PecMetricNames pecMetricNames = new PecMetricNames();

            @Getter
            @Setter
            public static class PecMetricNames {
                private String markMessageAsReadResponseTime;
                private String deleteMessageResponseTime;
                private String getUnreadMessagesResponseTime;
                private String getMessageCountResponseTime;
                private String sendMailResponseTime;
                private String payloadSizeRange;
                private String messageCountRange;
                private String getUnreadPecMessagesCount;
            }
        }

        @Getter
        @Setter
        public static class Endpoint {
            private Consolidatore consolidatore = new Consolidatore();
            private ExternalChannel externalChannel = new ExternalChannel();
            private GestoreRepository gestoreRepository = new GestoreRepository();
            private Files files = new Files();
            private SafeStorage safeStorage = new SafeStorage();

            @Getter
            @Setter
            public static class Consolidatore {
                private Boolean trustAll;
                private String baseUrl;
                private String basePath;
                private String clientHeaderName;
                private String clientHeaderValue;
                private String apiKeyHeaderName;
                private String apiKeyHeaderValue;
                private PaperMessages paperMessages = new PaperMessages();

                @Getter
                @Setter
                public static class PaperMessages {
                    private String putRequest;
                    private String putDuplicateRequest;
                    private String getRequestProgress;
                    private String getDuplicateRequestProgress;
                }
            }

            @Getter
            @Setter
            public static class ExternalChannel {
                private String containerBaseUrl;
            }

            @Getter
            @Setter
            public static class GestoreRepository {
                private String getClientConfiguration;
                private String postClientConfiguration;
                private String putClientConfiguration;
                private String deleteClientConfiguration;
                private String getRequest;
                private String postRequest;
                private String patchRequest;
                private String deleteRequest;
                private String getRequestByMessageId;
                private String postDiscardedEvents;
                private String setMessageIdInRequestMetadata;
                private String getRequestMetadataByMessageId;
                private String setRequestMetadataMessageId;
            }

            @Getter
            @Setter
            public static class Files {
                private String getFile;
                private String postFile;
            }

            @Getter
            @Setter
            public static class SafeStorage {
                private String containerBaseUrl;
                private String clientHeaderName;
                private String clientHeaderValue;
                private String apiKeyHeaderName;
                private String apiKeyHeaderValue;
                private String checksumValueHeaderName;
                private String traceIdHeaderName;
            }
        }
    }
}
