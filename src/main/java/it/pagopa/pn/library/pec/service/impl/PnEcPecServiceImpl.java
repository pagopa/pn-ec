package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.configurationproperties.PnPecRetryStrategyProperties;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.NamirialProviderMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.MaxRetriesExceededException;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecGetMessagesResponse;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecListOfMessages;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecMessage;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
    @Value("${library.pec.cloudwatch.namespace.aruba}")
    private String arubaProviderNamespace;
    @Value("${library.pec.cloudwatch.namespace.namirial}")
    private String namirialProviderNamespace;


    @Autowired
    public PnEcPecServiceImpl(@Qualifier("arubaServiceImpl") ArubaService arubaService, com.namirial.pec.library.service.PnPecServiceImpl namirialService, PnPecConfigurationProperties props,
                            PnPecRetryStrategyProperties retryStrategyProperties, CloudWatchPecMetrics cloudWatchPecMetrics) {
        this.arubaService = arubaService;
        this.namirialService = namirialService;
        this.retryStrategyProperties = retryStrategyProperties;
        this.props = props;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
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

    @Override
    public Mono<String> sendMail(byte[] message) {
        log.logStartingProcess(PN_EC_PEC_SEND_MAIL);
        PnPecService provider = getProviderWrite();
        return provider
                .sendMail(message)
                .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_SEND_MAIL, provider))
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_SEND_MAIL))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_SEND_MAIL, false, throwable.getMessage()));
    }

    @Override
    public Mono<PnEcPecGetMessagesResponse> getUnreadMessages(int limit) {
        log.logStartingProcess(PN_EC_PEC_GET_UNREAD_MESSAGES);

        return Flux.fromIterable(getProvidersRead())
                .flatMap(provider -> provider.getUnreadMessages(limit)
                        .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_GET_UNREAD_MESSAGES, provider))
                        .map(pnGetMessagesResponse -> Tuples.of(pnGetMessagesResponse, getProviderName(provider))))
                .flatMap(tuple -> {
                    var listOfMessages = tuple.getT1().getPnListOfMessages();
                    var providerName = tuple.getT2();
                    if (listOfMessages == null) {
                        return Flux.empty();
                    } else {
                        return Flux.fromIterable(listOfMessages.getMessages()).map(message -> new PnEcPecMessage(message, providerName));
                    }
                })
                .collectList()
                .flatMap(messages -> Mono.just(new PnEcPecGetMessagesResponse(messages.isEmpty() ? null : new PnEcPecListOfMessages(messages), messages.size())))
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_GET_UNREAD_MESSAGES))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_GET_UNREAD_MESSAGES, false, throwable.getMessage()));
    }

    @Override
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess(PN_EC_PEC_GET_MESSAGE_COUNT);
        return Flux.fromIterable(getProvidersRead())
                .flatMap(provider -> provider.getMessageCount()
                        .flatMap(count -> cloudWatchPecMetrics.publishMessageCount(Long.valueOf(count), getMetricNamespace(provider)).thenReturn(count))
                        .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_GET_MESSAGE_COUNT, provider)))
                .reduce(0, Integer::sum)
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_GET_MESSAGE_COUNT))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_GET_MESSAGE_COUNT, false, throwable.getMessage()));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID, String providerName) {
        log.logStartingProcess(PN_EC_PEC_MARK_MESSAGE_AS_READ);
        PnPecService provider = getProviderByName(providerName);
        return provider.markMessageAsRead(messageID)
                .retryWhen(getPnPecRetryStrategy(PN_EC_PEC_MARK_MESSAGE_AS_READ, provider))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PN_EC_PEC_MARK_MESSAGE_AS_READ))
                .doOnError(throwable -> log.logEndingProcess(PN_EC_PEC_MARK_MESSAGE_AS_READ, false, throwable.getMessage()));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID, String senderMessageID) {
        log.logStartingProcess(PN_EC_PEC_DELETE_MESSAGE);
        PnPecService provider = getProviderByMessageId(senderMessageID);
        return provider.deleteMessage(messageID)
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
            default -> {
                log.debug(ERROR_PARSING_PROPERTY_VALUES);
                throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES);
            }
        };
    }

    private List<PnPecService> getProvidersRead() {
        List<String> providers = props.getPnPecProviderSwitchRead();
        List<PnPecService> services = new ArrayList<>();
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
        } else {
            log.debug(NAMIRIAL_PROVIDER_SELECTED);
            return namirialService;
        }
    }

    private PnPecService getProviderByName(String providerName) {
        if (providerName.equals(ARUBA_PROVIDER)) {
            log.debug(ARUBA_PROVIDER_SELECTED);
            return arubaService;
        } else if (providerName.equals(NAMIRIAL_PROVIDER)) {
            log.debug(NAMIRIAL_PROVIDER_SELECTED);
            return namirialService;
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
        } else {
            log.debug(ERROR_RETRIEVING_METRIC_NAMESPACE);
            throw new IllegalArgumentException(ERROR_RETRIEVING_METRIC_NAMESPACE);
        }
    }

        public static boolean isAruba(String messageID) {
            return messageID.trim().toLowerCase().endsWith(ARUBA_PATTERN_STRING);
        }


    }

