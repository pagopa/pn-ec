package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.library.pec.exception.pecservice.ProvidersNotAvailableException;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.AlternativeProviderService;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@CustomLog
@Service
public class PnPecServiceImpl implements PnPecService {


    private final ArubaService arubaService;
    private final AlternativeProviderService otherService;
    private final PnPecConfigurationProperties props;

    private static final String ARUBA_PROVIDER = "aruba";
    private static final String OTHER_PROVIDER = "other";
    private static final String SERVICE_ERROR = "Error retrieving messages from service: {}";

    @Autowired
    public PnPecServiceImpl(@Qualifier("arubaServiceImpl") ArubaService arubaService,
                            @Qualifier("alternativeProviderServiceImpl") AlternativeProviderService otherService,
                            PnPecConfigurationProperties props) {
        this.arubaService = arubaService;
        this.otherService = otherService;
        this.props = props;
    }

    @Override
    public Mono<String> sendMail(byte[] message) {
        log.logStartingProcess(PN_PEC_SEND_MAIL);
        return getProvider()
                .sendMail(message)
                .doOnSuccess(result -> log.logEndingProcess(PN_PEC_SEND_MAIL))
                .doOnError(throwable -> log.logEndingProcess(PN_PEC_SEND_MAIL, false, throwable.getMessage()));
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {

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
                        return new PnGetMessagesResponse(new PnListOfMessages(messages), messages.size());
                    }
                })
                .doOnSubscribe(subscription -> log.logStartingProcess(PEC_GET_UNREAD_MESSAGES))
                .doOnSuccess(result -> log.logEndingProcess(PEC_GET_UNREAD_MESSAGES));

    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        log.logStartingProcess(PEC_MARK_MESSAGE_AS_READ);
        PnPecService provider = getProvider(messageID);
        return provider.markMessageAsRead(messageID)
                .then()
                .doOnSuccess(result -> log.logEndingProcess(PEC_MARK_MESSAGE_AS_READ))
                .doOnError(throwable -> log.logEndingProcess(PEC_MARK_MESSAGE_AS_READ, false, throwable.getMessage()));
    }

    @Override
    public Mono<Integer> getMessageCount() {
        log.logStartingProcess(PEC_GET_MESSAGE_COUNT);

        AtomicBoolean isArubaOk = new AtomicBoolean(true);
        AtomicBoolean isOtherOk = new AtomicBoolean(true);

        Mono<Integer> arubaCount = arubaService.getMessageCount()
                .onErrorResume(e -> {
                    log.warn(SERVICE_ERROR, "ArubaService", e);
                    isArubaOk.set(false);
                    return Mono.just(0);
                });

        Mono<Integer> otherProviderCount = otherService.getMessageCount()
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
    public Mono<Void> deleteMessage(String messageID) {
        log.logStartingProcess(PEC_DELETE_MESSAGE);

        PnPecService provider = getProvider(messageID);

        return provider.deleteMessage(messageID)
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
