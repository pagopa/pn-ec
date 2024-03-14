package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.library.pec.configurationproperties.PnPecRetryStrategyProperties;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.AlternativeProviderMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.MaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.AlternativeProviderService;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@CustomLog
@Service
public class PnPecServiceImpl implements PnPecService {

    private final ArubaService arubaService;
    private final AlternativeProviderService otherService;
    private final PnPecConfigurationProperties props;
    private final PnPecRetryStrategyProperties retryStrategyProperties;

    private static final String ARUBA_PROVIDER = "aruba";
    private static final String OTHER_PROVIDER = "other";
    private static final String SERVICE_ERROR = "Error retrieving messages from service: {}";
    private static final String RETRIES_EXCEEDED_MESSAGE = "Max retries exceeded for ";
    private static final String ARUBA_PROVIDER_SELECTED = "Aruba provider selected";
    private static final String OTHER_PROVIDER_SELECTED = "Other provider selected";
    private static final String ERROR_PARSING_PROPERTY_VALUES = "Error parsing property values, wrong value for service";



    @Autowired
    public PnPecServiceImpl(@Qualifier("arubaServiceImpl") ArubaService arubaService,
                            @Qualifier("alternativeProviderServiceImpl") AlternativeProviderService otherService,
                            PnPecConfigurationProperties props,
                            PnPecRetryStrategyProperties retryStrategyProperties) {
        this.arubaService = arubaService;
        this.retryStrategyProperties = retryStrategyProperties;
        this.otherService = otherService;
        this.props = props;
    }

    private Retry getPnPecRetryStrategy(String clientMethodName, String providerName) {
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return Retry.backoff(Long.parseLong(retryStrategyProperties.maxAttempts()), Duration.ofSeconds(Long.parseLong(retryStrategyProperties.minBackoff())))
                .filter(PnSpapiTemporaryErrorException.class::isInstance)
                .doBeforeRetry(retrySignal -> {
                    MDCUtils.enrichWithMDC(null, mdcContextMap);
                    log.debug("Retry number {} for '{}', caused by : {}", retrySignal.totalRetries(), clientMethodName, retrySignal.failure().getMessage(), retrySignal.failure());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    if (providerName.equals(ARUBA_PROVIDER)) {
                        return new ArubaCallMaxRetriesExceededException(RETRIES_EXCEEDED_MESSAGE + clientMethodName, retrySignal.failure());
                    } else if (providerName.equals(OTHER_PROVIDER)) {
                        return new AlternativeProviderMaxRetriesExceededException(RETRIES_EXCEEDED_MESSAGE + clientMethodName, retrySignal.failure());
                    } else {
                        return new MaxRetriesExceededException(RETRIES_EXCEEDED_MESSAGE + clientMethodName, retrySignal.failure());
                    }
                });
    }

    @Override
    @SneakyThrows
    public Mono<String> sendMail(byte[] message) {
        log.logStartingProcess(PN_PEC_SEND_MAIL);
        return getProviderWrite()
                .sendMail(message)
                .retryWhen(getPnPecRetryStrategy(PN_PEC_SEND_MAIL, props.getPnPecProviderSwitchWrite()))
                .doOnSuccess(result -> log.logEndingProcess(PN_PEC_SEND_MAIL))
                .doOnError(throwable -> log.logEndingProcess(PN_PEC_SEND_MAIL, false, throwable.getMessage()));
    }

    @Override
    @SneakyThrows
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        log.logStartingProcess(PEC_GET_UNREAD_MESSAGES);

        return Flux.fromIterable(getProvidersRead())
                .flatMap(provider -> provider.getUnreadMessages(limit)
                        .retryWhen(getPnPecRetryStrategy(PEC_GET_UNREAD_MESSAGES, provider.getClass().getName())))
                .flatMap(response -> {
                    var listOfMessages = response.getPnListOfMessages();
                    if (listOfMessages == null) {
                        return Flux.empty();
                    } else {
                        return Flux.fromIterable(listOfMessages.getMessages());
                    }
                })
                .parallel().runOn(Schedulers.parallel())
                .sequential()
                .collectList()
                .flatMap(messages -> Mono.just(new PnGetMessagesResponse(messages.isEmpty() ? null : new PnListOfMessages(messages), messages.size()))
                )
                .doOnSuccess(result -> log.logEndingProcess(PEC_GET_UNREAD_MESSAGES))
                .onErrorResume(e -> {
                    log.fatal(SERVICE_ERROR, e);
                    return Mono.error(e);
                });
    }



    @Override
    @SneakyThrows
    public Mono<Void> markMessageAsRead(String messageID) {
        log.logStartingProcess(PEC_MARK_MESSAGE_AS_READ);
        PnPecService provider = getProvider(messageID);
        return provider.markMessageAsRead(messageID)
                .retryWhen(getPnPecRetryStrategy(PEC_MARK_MESSAGE_AS_READ, props.getPnPecProviderSwitchWrite()))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PEC_MARK_MESSAGE_AS_READ))
                .doOnError(throwable -> log.logEndingProcess(PEC_MARK_MESSAGE_AS_READ, false, throwable.getMessage()));
    }

    @Override
    @SneakyThrows
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess(PEC_GET_MESSAGE_COUNT);

        return Flux.fromIterable(getProvidersRead())
                .flatMap(provider -> provider.getMessageCount()
                        .retryWhen(getPnPecRetryStrategy(PEC_GET_MESSAGE_COUNT, provider.getClass().getName())))
                .parallel().runOn(Schedulers.parallel())
                .sequential()
                .reduce(0, Integer::sum)
                .doOnSuccess(result -> log.logEndingProcess(PEC_GET_MESSAGE_COUNT))
                .onErrorResume(e -> {
                    log.fatal(SERVICE_ERROR, e);
                    return Mono.error(e);
                });
    }


    @Override
    @SneakyThrows
    public Mono<Void> deleteMessage(String messageID) {
        log.logStartingProcess(PEC_DELETE_MESSAGE);

        PnPecService provider = getProvider(messageID);

        return provider.deleteMessage(messageID)
                .retryWhen(getPnPecRetryStrategy(PEC_DELETE_MESSAGE, props.getPnPecProviderSwitchWrite()))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PEC_DELETE_MESSAGE))
                .doOnError(throwable -> log.logEndingProcess(PEC_DELETE_MESSAGE, false, throwable.getMessage()));
    }


    private PnPecService getProviderWrite() {
        return switch (props.getPnPecProviderSwitchWrite()) {
            case ARUBA_PROVIDER -> {
                log.debug(ARUBA_PROVIDER_SELECTED);
                yield arubaService;
            }
            case OTHER_PROVIDER -> {
                log.debug(OTHER_PROVIDER_SELECTED);
                yield otherService;
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
            } else if (provider.equals(OTHER_PROVIDER)) {
                log.debug(OTHER_PROVIDER_SELECTED);
                services.add(arubaService);
            } else {
                log.debug(ERROR_PARSING_PROPERTY_VALUES);
                throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES + " : " + provider);
            }
        }
        return services;
    }

        private PnPecService getProvider(String messageID) {
         if (ArubaServiceImpl.isAruba(messageID)) {
             log.debug(ARUBA_PROVIDER_SELECTED);
             return arubaService;
         } else if (AlternativeProviderServiceImpl.isOther(messageID)){
             log.debug(OTHER_PROVIDER_SELECTED);
             return otherService;
         } else {
             log.debug(ERROR_PARSING_PROPERTY_VALUES);
             throw new IllegalArgumentException(ERROR_PARSING_PROPERTY_VALUES);
         }
        }
    }

