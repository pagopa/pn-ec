package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.library.pec.configurationproperties.PnPecRetryStrategyProperties;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.AlternativeProviderMaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.MaxRetriesExceededException;
import it.pagopa.pn.library.pec.exception.pecservice.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.exception.pecservice.ProvidersNotAvailableException;
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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->{
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
        return getProvider()
                .sendMail(message)
                .retryWhen(getPnPecRetryStrategy(PN_PEC_SEND_MAIL, props.getPnPecProviderSwitch()))
                .doOnSuccess(result -> log.logEndingProcess(PN_PEC_SEND_MAIL))
                .doOnError(throwable -> log.logEndingProcess(PN_PEC_SEND_MAIL, false, throwable.getMessage()));
    }

    @Override
    @SneakyThrows
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        log.logStartingProcess(PEC_GET_UNREAD_MESSAGES);

        AtomicBoolean isArubaOk = new AtomicBoolean(true);
        AtomicBoolean isOtherOk = new AtomicBoolean(true);

        Flux<byte[]> arubaMessages = arubaService.getUnreadMessages(limit)
                .flatMapMany(response -> {
                    var listOfMessages = response.getPnListOfMessages();
                    if (Objects.isNull(listOfMessages)) {
                        return Flux.empty();
                    } else {
                        return Flux.fromIterable(listOfMessages.getMessages());
                    }
                })
                .retryWhen(getPnPecRetryStrategy(PEC_GET_UNREAD_MESSAGES, ARUBA_PROVIDER))
                .onErrorResume(e -> {
                    log.warn(SERVICE_ERROR, "ArubaService", e);
                    isArubaOk.set(false);
                    return Flux.empty();
                });

        Flux<byte[]> otherProviderMessages = otherService.getUnreadMessages(limit)
                .flatMapMany(response -> {
                    var listOfMessages = response.getPnListOfMessages();
                    if (Objects.isNull(listOfMessages)) {
                        return Flux.empty();
                    } else {
                        return Flux.fromIterable(listOfMessages.getMessages());
                    }
                })
                .retryWhen(getPnPecRetryStrategy(PEC_GET_UNREAD_MESSAGES, OTHER_PROVIDER))
                .onErrorResume(e -> {
                    log.warn(SERVICE_ERROR, "OtherProviderService", e);
                    isOtherOk.set(false);
                    return Flux.empty();
                });

        return Flux.merge(arubaMessages, otherProviderMessages)
                .collectList()
                .map(messages -> {
                    if (!isArubaOk.get() && !isOtherOk.get()) {
                        throw new ProvidersNotAvailableException("Both services returned an error");
                    } else {
                        return new PnGetMessagesResponse(new PnListOfMessages(messages.isEmpty() ? null : messages), messages.size());
                    }
                })
                .doOnSuccess(result -> log.logEndingProcess(PEC_GET_UNREAD_MESSAGES));

    }

    @Override
    @SneakyThrows
    public Mono<Void> markMessageAsRead(String messageID) {
        log.logStartingProcess(PEC_MARK_MESSAGE_AS_READ);
        PnPecService provider = getProvider(messageID);
        return provider.markMessageAsRead(messageID)
                .retryWhen(getPnPecRetryStrategy(PEC_MARK_MESSAGE_AS_READ, props.getPnPecProviderSwitch()))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PEC_MARK_MESSAGE_AS_READ))
                .doOnError(throwable -> log.logEndingProcess(PEC_MARK_MESSAGE_AS_READ, false, throwable.getMessage()));
    }

    @Override
    @SneakyThrows
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess(PEC_GET_MESSAGE_COUNT);

        AtomicBoolean isArubaOk = new AtomicBoolean(true);
        AtomicBoolean isOtherOk = new AtomicBoolean(true);

        Mono<Integer> arubaCount = arubaService.getMessageCount()
                .retryWhen(getPnPecRetryStrategy(PEC_GET_MESSAGE_COUNT, ARUBA_PROVIDER))
                .onErrorResume(e -> {
                    log.warn(SERVICE_ERROR, "ArubaService", e);
                    isArubaOk.set(false);
                    return Mono.just(0);
                });

        Mono<Integer> otherProviderCount = otherService.getMessageCount()
                .retryWhen(getPnPecRetryStrategy(PEC_GET_MESSAGE_COUNT, OTHER_PROVIDER))
                .onErrorResume(e -> {
                    log.warn(SERVICE_ERROR, "OtherProviderService", e);
                    isOtherOk.set(false);
                    return Mono.just(0);
                });

        return Mono.zip(arubaCount, otherProviderCount)
                .map(tuple -> {
                    if (!isArubaOk.get() && !isOtherOk.get()) {
                        throw new ProvidersNotAvailableException("Both services returned an error");
                    }  else {
                        return tuple.getT1() + tuple.getT2();
                    }
                })
                .doOnError(throwable -> log.logEndingProcess(PEC_GET_MESSAGE_COUNT, false, throwable.getMessage()))
                .doOnSuccess(result -> log.logEndingProcess(PEC_GET_MESSAGE_COUNT));
    }

    @Override
    @SneakyThrows
    public Mono<Void> deleteMessage(String messageID){
        log.logStartingProcess(PEC_DELETE_MESSAGE);

        PnPecService provider = getProvider(messageID);

        return provider.deleteMessage(messageID)
                .retryWhen(getPnPecRetryStrategy(PEC_DELETE_MESSAGE,props.getPnPecProviderSwitch()))
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PEC_DELETE_MESSAGE))
                .doOnError(throwable -> log.logEndingProcess(PEC_DELETE_MESSAGE, false, throwable.getMessage()));
    }


    private PnPecService getProvider() {
        if (props.getPnPecProviderSwitch().equals(ARUBA_PROVIDER)) {
            log.debug("Aruba provider selected");
            return arubaService;
        } else if (props.getPnPecProviderSwitch().equals(OTHER_PROVIDER)) {
            log.debug("Other provider selected");
            return otherService;
        } else {
            log.debug("Error parsing property values, wrong value for service.");
            throw new IllegalArgumentException("Error parsing property values, wrong value for service.");
        }
    }

    private PnPecService getProvider(String messageID) {
        if (ArubaServiceImpl.isAruba(messageID)) {
            return arubaService;
        } else {
            return otherService;
        }
    }
}
