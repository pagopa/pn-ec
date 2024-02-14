package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
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
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@CustomLog
@Service
public class PnPecServiceImpl implements PnPecService {

    @Autowired
    @Qualifier("arubaServiceImpl")
    private ArubaService arubaService;

    @Autowired
    @Qualifier("alternativeProviderServiceImpl")
    private AlternativeProviderService otherService;

    @Autowired
    PnPecConfigurationProperties props;

    private static final String ARUBA_PROVIDER = "aruba";
    //TODO cambiare valore provider una volta che sarà disponibile il servizio
    private static final String OTHER_PROVIDER = "other";

    @Override
    public Mono<String> sendMail(byte[] message) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_SEND_MAIL, message);
        return getProvider()
                .sendMail(message)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, PEC_SEND_MAIL, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_SEND_MAIL, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_GET_UNREAD_MESSAGES, limit);

        Flux<byte[]> arubaMessages = arubaService.getUnreadMessages(limit)
                .flatMapIterable(response -> response.getPnListOfMessages().getMessages())
                .onErrorResume(e -> {
                    log.error("Error retrieving messages from ArubaService", e);
                    return Flux.empty();
                });

        Flux<byte[]> otherProviderMessages = otherService.getUnreadMessages(limit)
                .flatMapIterable(response -> response.getPnListOfMessages().getMessages())
                .onErrorResume(e -> {
                    log.error("Error retrieving messages from ArubaService", e);
                    return Flux.empty();
                });

        return Flux.merge(arubaMessages, otherProviderMessages)
                .collectList()
                .map(messages -> {
                    PnGetMessagesResponse pnGetMessagesResponse = new PnGetMessagesResponse();
                    pnGetMessagesResponse.setPnListOfMessages(new PnListOfMessages(messages));
                    pnGetMessagesResponse.setNumOfMessages(messages.size());
                    return pnGetMessagesResponse;
                })
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_GET_UNREAD_MESSAGES, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_GET_UNREAD_MESSAGES, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_MARK_MESSAGE_AS_READ, messageID);
        PnPecService provider = getProvider(messageID);
        return provider.markMessageAsRead(messageID)
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, PEC_MARK_MESSAGE_AS_READ, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_MARK_MESSAGE_AS_READ, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<Integer> getMessageCount() {
        log.debug(INVOKING_OPERATION_LABEL, PEC_GET_MESSAGE_COUNT);

        Mono<Integer> arubaCount = arubaService.getMessageCount()
                .onErrorResume(e -> {
                    //TODO verificare livello di log
                    log.error("Error retrieving messages from ArubaService", e);
                    return Mono.just(0);
                });
        Mono<Integer> otherProviderCount = otherService.getMessageCount()
                .onErrorResume(e -> {
                    //TODO verificare livello di log
                    log.error("Error retrieving messages from ArubaService", e);
                    return Mono.just(0);
                });

        return Mono.just(arubaCount.block()+otherProviderCount.block())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_GET_MESSAGE_COUNT, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_GET_MESSAGE_COUNT, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_DELETE_MESSAGE, messageID);

        PnPecService provider = getProvider(messageID);

        return provider.deleteMessage(messageID)
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, PEC_DELETE_MESSAGE, messageID))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS_FOR, PEC_DELETE_MESSAGE, messageID, throwable, throwable.getMessage()));
    }


    private PnPecService getProvider(){
        if(props.getPnPecProviderSwitch().equals(ARUBA_PROVIDER)){
            //TODO verificare livello di log
            log.debug("Aruba provider selected");
            return arubaService;
        }else if (props.getPnPecProviderSwitch().equals(OTHER_PROVIDER)) {
            //TODO verificare livello di log
            log.debug("Other provider selected");
            return otherService;
        }else{
            log.debug("Error parsing property values, wrong value for service.");
            throw new IllegalArgumentException("Error parsing property values, wrong value for service.");
        }
    }

    private PnPecService getProvider(String messageID){
        if(ArubaServiceImpl.isAruba(messageID)){
            return arubaService;
        }else{
            return otherService;
        }
    }
}
