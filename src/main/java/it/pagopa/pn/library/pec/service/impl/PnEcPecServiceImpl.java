package it.pagopa.pn.library.pec.service.impl;

import com.namirial.pec.library.service.PnPecServiceImpl;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.dummy.pec.service.DummyPecService;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.configuration.MetricsDimensionConfiguration;
import it.pagopa.pn.library.pec.configurationproperties.PnPecMetricNames;
import it.pagopa.pn.library.pec.configurationproperties.PnPecRetryStrategyProperties;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.NamirialProviderMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.MaxRetriesExceededException;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecGetMessagesResponse;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecListOfMessages;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecMessage;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pagopa.pn.library.pec.utils.PnPecUtils;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.*;

@CustomLog
@Service
public class PnEcPecServiceImpl implements PnEcPecService {

    private final ArubaService arubaService;
    private final com.namirial.pec.library.service.PnPecServiceImpl namirialService;
    private final PnPecConfigurationProperties props;
    private final PnPecRetryStrategyProperties retryStrategyProperties;
    private final CloudWatchPecMetrics cloudWatchPecMetrics;
    private final DummyPecService dummyPecService;
    private final PnPecMetricNames pnPecMetricNames;
    private final MetricsDimensionConfiguration metricsDimensionConfiguration;
    private static final Logger jsonLogger = LoggerFactory.getLogger("it.pagopa.pn.JsonLogger");

    @Value("${library.pec.cloudwatch.namespace.aruba}")
    private String arubaProviderNamespace;
    @Value("${library.pec.cloudwatch.namespace.namirial}")
    private String namirialProviderNamespace;

    @Autowired
    public PnEcPecServiceImpl(@Qualifier("arubaServiceImpl") ArubaService arubaService, PnPecServiceImpl namirialService, PnPecConfigurationProperties props,
                              PnPecRetryStrategyProperties retryStrategyProperties, CloudWatchPecMetrics cloudWatchPecMetrics, PnPecMetricNames pnPecMetricNames,
                              MetricsDimensionConfiguration metricsDimensionConfiguration, DummyPecService dummyPecService) {
        this.arubaService = arubaService;
        this.namirialService = namirialService;
        this.retryStrategyProperties = retryStrategyProperties;
        this.props = props;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.dummyPecService = dummyPecService;
        this.pnPecMetricNames = pnPecMetricNames;
        this.metricsDimensionConfiguration = metricsDimensionConfiguration;
    }

    private Retry getPnPecRetryStrategy(String clientMethodName, PnPecService service) {
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return Retry.backoff(Long.parseLong(retryStrategyProperties.maxAttempts()), Duration.ofSeconds(Long.parseLong(retryStrategyProperties.minBackoff())))
                .filter(PnSpapiTemporaryErrorException.class::isInstance)
                .doBeforeRetry(retrySignal -> {
                    MDCUtils.enrichWithMDC(null, mdcContextMap);
                    log.debug("Retry number {} for '{}', caused by : {}", retrySignal.totalRetries(), clientMethodName, retrySignal.failure().getMessage(), retrySignal.failure());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    if (service instanceof ArubaService) {
                        return new ArubaCallMaxRetriesExceededException(RETRIES_EXCEEDED_MESSAGE + clientMethodName, retrySignal.failure());
                    } else if (service instanceof com.namirial.pec.library.service.PnPecServiceImpl) {
                        return new NamirialProviderMaxRetriesExceededException(RETRIES_EXCEEDED_MESSAGE + clientMethodName, retrySignal.failure());
                    } else {
                        return new MaxRetriesExceededException(RETRIES_EXCEEDED_MESSAGE + clientMethodName, retrySignal.failure());
                    }
                });
    }


    /**
     * A function to execute the sendMail operations and handle related metrics.
     */
    private Function<Mono<PnPecService>, Mono<String>> sendMailAndHandleMetrics(byte[] message) {
        Dimension payloadSizeRangeDimension = metricsDimensionConfiguration.getDimension(pnPecMetricNames.getPayloadSizeRange(), (long) message.length);
        return tFlux -> tFlux.flatMap(provider -> cloudWatchPecMetrics.executeAndPublishResponseTime(provider.sendMail(message), getMetricNamespace(provider), pnPecMetricNames.getSendMailResponseTime(), payloadSizeRangeDimension)
                .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_SEND_MAIL, provider)));
    }

    @Override
    public Mono<String> sendMail(byte[] message) {
        log.logStartingProcess(PN_EC_PEC_SEND_MAIL);
        return Mono.fromSupplier(this::getProviderWrite)
                .transform(sendMailAndHandleMetrics(message))
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_SEND_MAIL))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_SEND_MAIL, false, throwable.getMessage()));
    }

    /**
     * A function to execute the getUnreadMessages operations and handle related metrics.
     */
    private Function<Flux<PnPecService>, Flux<PnEcPecMessage>> getUnreadMessagesAndHandleMetrics(int limit) {
        return tFlux -> tFlux.flatMap(provider -> cloudWatchPecMetrics.executeAndPublishResponseTime(provider.getUnreadMessages(limit), getMetricNamespace(provider), pnPecMetricNames.getGetUnreadMessagesResponseTime())
                        .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_GET_UNREAD_MESSAGES, provider))
                        .map(pnGetMessagesResponse -> Tuples.of(pnGetMessagesResponse, getProviderName(provider))))
                .transform(handleGetUnreadMessagesResponses());
    }

    /**
     * A function to handle the responses from the getUnreadMessages operations
     */
    private Function<Flux<Tuple2<PnGetMessagesResponse, String>>, Flux<PnEcPecMessage>> handleGetUnreadMessagesResponses() {
        return tFlux -> tFlux.flatMap(tuple -> {
            var listOfMessages = tuple.getT1().getPnListOfMessages();
            var providerName = tuple.getT2();
            if (listOfMessages == null) {
                return Flux.empty();
            } else {
                return Flux.fromIterable(listOfMessages.getMessages()).map(message -> new PnEcPecMessage(message, providerName));
            }
        });
    }

    @Override
    public Mono<PnEcPecGetMessagesResponse> getUnreadMessages(int limit) {
        log.logStartingProcess(PN_EC_PEC_GET_UNREAD_MESSAGES);

        return Flux.fromIterable(getProvidersRead())
                .transform(getUnreadMessagesAndHandleMetrics(limit))
                .collectList()
                .flatMap(this::processAndLogUnreadPecMessages)
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_GET_UNREAD_MESSAGES))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_GET_UNREAD_MESSAGES, false, throwable.getMessage()));
    }

    private Mono<PnEcPecGetMessagesResponse> processAndLogUnreadPecMessages(List<PnEcPecMessage> messages) {
        log.info(INVOKING_OPERATION_LABEL, UNREAD_PEC_MESSAGE);
        if (messages.isEmpty()) {
            getProvidersRead().stream().findFirst()
                    .ifPresentOrElse(
                            provider -> jsonLogger.info(PnPecUtils.createEmfJson(getMetricNamespace(provider),
                                    pnPecMetricNames.getGetUnreadPecMessagesCount(), 0L)),
                            () -> log.warn("No providers available to log metrics.")
                    );
            return Mono.just(new PnEcPecGetMessagesResponse(null, 0));
        }
        messages.stream()
                .collect(Collectors.groupingBy(PnEcPecMessage::getProviderName, Collectors.counting()))
                .forEach((providerName, count) -> jsonLogger.info(PnPecUtils.createEmfJson(
                        getMetricNamespace(getProviderByName(providerName)),
                        pnPecMetricNames.getGetUnreadPecMessagesCount(), count)));

        return Mono.just(new PnEcPecGetMessagesResponse(new PnEcPecListOfMessages(messages), messages.size()));
    }



    /**
     * A function to execute the getMessageCount operation and handle related metrics.
     */
    private Function<Flux<PnPecService>, Flux<Integer>> getMessageCountAndHandleMetrics() {
        return tFlux -> tFlux.flatMap(provider -> cloudWatchPecMetrics.executeAndPublishResponseTime(provider.getMessageCount(), getMetricNamespace(provider), pnPecMetricNames.getGetMessageCountResponseTime())
                .flatMap(count -> cloudWatchPecMetrics.publishMessageCount(Long.valueOf(count), getMetricNamespace(provider)).thenReturn(count))
                .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_GET_MESSAGE_COUNT, provider)));
    }

    @Override
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess(PN_EC_PEC_GET_MESSAGE_COUNT);
        return Flux.fromIterable(getProvidersRead())
                .transform(getMessageCountAndHandleMetrics())
                .reduce(0, Integer::sum)
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_GET_MESSAGE_COUNT))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_GET_MESSAGE_COUNT, false, throwable.getMessage()));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID, String providerName) {
        log.logStartingProcess(PN_EC_PEC_MARK_MESSAGE_AS_READ);
        PnPecService provider = getProviderByName(providerName);
        return cloudWatchPecMetrics.executeAndPublishResponseTime(provider.markMessageAsRead(messageID), getMetricNamespace(provider), pnPecMetricNames.getMarkMessageAsReadResponseTime())
                .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_MARK_MESSAGE_AS_READ, provider))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_MARK_MESSAGE_AS_READ))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_MARK_MESSAGE_AS_READ, false, throwable.getMessage()));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID, String senderMessageID) {
        log.logStartingProcess(PN_EC_PEC_DELETE_MESSAGE);
        PnPecService provider = getProviderByMessageId(senderMessageID);
        return cloudWatchPecMetrics.executeAndPublishResponseTime(provider.deleteMessage(messageID), getMetricNamespace(provider), pnPecMetricNames.getDeleteMessageResponseTime())
                .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_DELETE_MESSAGE, provider))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_DELETE_MESSAGE))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_DELETE_MESSAGE, false, throwable.getMessage()));
    }


    private PnPecService getProviderWrite() {
        return switch (props.getPnPecProviderSwitchWrite()) {
            case ARUBA_PROVIDER -> {
                log.debug(ARUBA_PROVIDER_SELECTED);
                yield arubaService;
            }
            case NAMIRIAL_PROVIDER -> {
                log.debug(NAMIRIAL_PROVIDER_SELECTED);
                yield namirialService;
            }
            case DUMMY_PROVIDER -> {
                log.debug(DUMMY_PROVIDER_SELECTED);
                yield dummyPecService;
            }
            default -> {
                log.debug(ERROR_PARSING_PROPERTY_VALUES);
                throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES);
            }
        };
    }

    private List<PnPecService> getProvidersRead() {
        List<String> providers = props.getPnPecProviderSwitchRead();
        List<PnPecService> services = new ArrayList<>();
        if (providers.contains(DUMMY_PROVIDER)) {
            return new ArrayList<>(List.of(dummyPecService));
        }
        for (String provider : providers) {
            if (provider.equals(ARUBA_PROVIDER)) {
                log.debug(ARUBA_PROVIDER_SELECTED);
                services.add(arubaService);
            } else if (provider.equals(NAMIRIAL_PROVIDER)) {
                log.debug(NAMIRIAL_PROVIDER_SELECTED);
                services.add(namirialService);
            } else {
                log.debug(ERROR_PARSING_PROPERTY_VALUES);
                throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES + " : " + provider);
            }
        }
        return services;
    }

    private PnPecService getProviderByMessageId(String messageID) {
        if (isAruba(messageID)) {
            log.debug(ARUBA_PROVIDER_SELECTED);
            return arubaService;
        } else if (isNamirial(messageID)) {
            log.debug(NAMIRIAL_PROVIDER_SELECTED);
            return namirialService;
        } else if (isDummy(messageID)) {
            log.debug(DUMMY_PROVIDER_SELECTED);
            return dummyPecService;
        } else {
            log.debug(ERROR_PARSING_PROPERTY_VALUES);
            throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES);
        }
    }

    private PnPecService getProviderByName(String providerName) {
        if (providerName.equals(ARUBA_PROVIDER)) {
            log.debug(ARUBA_PROVIDER_SELECTED);
            return arubaService;
        } else if (providerName.equals(NAMIRIAL_PROVIDER)) {
            log.debug(NAMIRIAL_PROVIDER_SELECTED);
            return namirialService;
        } else if (providerName.equals(DUMMY_PROVIDER)) {
            log.debug(DUMMY_PROVIDER_SELECTED);
            return dummyPecService;
        } else {
            log.debug(ERROR_PARSING_PROPERTY_VALUES);
            throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES);
        }
    }

    private String getProviderName(PnPecService service) {
        if (service instanceof ArubaService) {
            return ARUBA_PROVIDER;
        } else if (service instanceof com.namirial.pec.library.service.PnPecServiceImpl) {
            return NAMIRIAL_PROVIDER;
        } else if (service instanceof DummyPecService) {
            return DUMMY_PROVIDER;
        } else {
            log.debug(ERROR_PARSING_PROPERTY_VALUES);
            throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES);
        }
    }

    private String getMetricNamespace(PnPecService service) {
        if (service instanceof ArubaService) {
            return arubaProviderNamespace;
        } else if (service instanceof com.namirial.pec.library.service.PnPecServiceImpl) {
            return namirialProviderNamespace;
        } else if (service instanceof DummyPecService) {
            return DUMMY_PROVIDER_NAMESPACE;
        } else {
            log.debug(ERROR_RETRIEVING_METRIC_NAMESPACE);
            throw new IllegalArgumentException(ERROR_RETRIEVING_METRIC_NAMESPACE);
        }
    }

        public static boolean isAruba(String messageID) {
            return messageID.trim().toLowerCase().endsWith(ARUBA_PATTERN_STRING);
        }

        public static boolean isDummy(String messageID) {
            return messageID.trim().toLowerCase().endsWith(DUMMY_PATTERN_STRING);
        }

        public static boolean isNamirial(String messageID) {
            return messageID.trim().toLowerCase().endsWith(NAMIRIAL_PATTERN_STRING);
        }


    }

