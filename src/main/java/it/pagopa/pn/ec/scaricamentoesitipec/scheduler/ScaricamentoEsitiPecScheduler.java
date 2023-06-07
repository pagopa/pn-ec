package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.ReactorUtils;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pec.bridgews.*;
import it.pec.daticert.Postacert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Objects;
import java.util.function.Predicate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.DOMAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;
@Component
@Slf4j
public class ScaricamentoEsitiPecScheduler {

    private final ArubaCall arubaCall;
    private final DaticertService daticertService;
    private final SqsService sqsService;

    @Value("${scaricamento-esiti-pec.get-messages.limit}")
    private String scaricamentoEsitiPecGetMessagesLimit;

    @Value("${sqs.queue.pec.scaricamento-esiti-name}")
    private String scaricamentoEsitiPecQueue;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall, DaticertService daticertService, SqsService sqsService) {
        this.arubaCall = arubaCall;
        this.daticertService = daticertService;
        this.sqsService = sqsService;
    }

    private final Predicate<Postacert> isPostaCertificataPredicate = postacert -> postacert.getTipo().equals(POSTA_CERTIFICATA);
    private final Predicate<Postacert> endsWithDomainPredicate = postacert -> postacert.getDati().getMsgid().endsWith(DOMAIN);

    private GetMessageID createGetMessageIdRequest(String messageID, Integer isuid, boolean markSeen) {
        var getMessageID = new GetMessageID();
        getMessageID.setMailid(messageID);
        getMessageID.setIsuid(isuid);
        getMessageID.setMarkseen(markSeen ? 1 : 0);
        return getMessageID;
    }

    @Scheduled(cron = "${cron.value.scaricamento-esiti-pec}")
    public void scaricamentoEsitiPecScheduler() {

        log.info("<-- SCARICAMENTO ESITI PEC SCHEDULER -->");

        var getMessages = new GetMessages();
        getMessages.setUnseen(1);
        getMessages.setOuttype(2);
        getMessages.setLimit(Integer.valueOf(scaricamentoEsitiPecGetMessagesLimit));

        arubaCall.getMessages(getMessages)
                .doOnError(ArubaCallMaxRetriesExceededException.class, e -> log.debug("Aruba non risponde. Circuit breaker"))
                .onErrorComplete(ArubaCallMaxRetriesExceededException.class)
                .flatMap(getMessagesResponse -> Mono.justOrEmpty(getMessagesResponse.getArrayOfMessages()))
                .flatMapIterable(MesArrayOfMessages::getItem)
                .flatMap(message -> {

                    var mimeMessage = getMimeMessage(message);
                    var messageID = getMessageIdFromMimeMessage(mimeMessage);
                    //Rimozione delle parentesi angolari dal messageID
                    var finalMessageID = messageID.substring(1, messageID.length() - 1);
                    var attachBytes = findAttachmentByName(mimeMessage, "daticert.xml");

                    log.debug("Try to download PEC '{}' daticert.xml", finalMessageID);

//                  Check se daticert.xml è presente controllando la lunghezza del byte[]
                    if (!Objects.isNull(attachBytes) && attachBytes.length > 0) {

                        log.debug("PEC {} has daticert.xml with content : {}", finalMessageID, new String(attachBytes));

//                      Deserialize daticert.xml. Start a new Mono inside the flatMap
                         return Mono.fromCallable(() -> daticertService.getPostacertFromByteArray(attachBytes))
//                                 Escludere questi daticert. Non sono delle 'comunicazione esiti'
                                .filter(isPostaCertificataPredicate.negate())

//                                 msgid arriva all'interno di due angolari <msgid>. Eliminare il primo e l'ultimo carattere
                                .map(postacert -> {
                                    var dati = postacert.getDati();
                                    var msgId = dati.getMsgid();
                                    dati.setMsgid(msgId.substring(1, msgId.length() - 1));
                                    log.debug("PEC {} has {} msgId", finalMessageID, msgId);
                                    return postacert;
                                })

//                               Escludere questi daticert. Non avendo il msgid terminante con il dominio pago non sono state inviate
//                               da noi
                                .filter(endsWithDomainPredicate)

//                               Daticert filtrati
                                .doOnDiscard(Postacert.class, postacert -> {
                                    if (isPostaCertificataPredicate.test(postacert)) {
                                        log.debug("PEC {} discarded, is {}", finalMessageID, POSTA_CERTIFICATA);
                                    } else if (!endsWithDomainPredicate.test(postacert)) {
                                        log.debug("PEC {} discarded, it was not sent by us", finalMessageID);
                                    }
                                })
                                .flatMap(postacert -> sqsService.send(scaricamentoEsitiPecQueue, finalMessageID, RicezioneEsitiPecDto.builder()
                                        .message(message)
                                        .daticert(attachBytes)
                                        .build()))
                                .thenReturn(finalMessageID);
                    }
                    else return Mono.just(finalMessageID);
                })
                //Marca il messaggio come letto.
                .flatMap(finalMessageID -> arubaCall.getMessageId(createGetMessageIdRequest(finalMessageID, 2, true)))
                .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .onErrorResume(throwable -> Mono.empty())
                .transform(pullFromFluxUntilIsEmpty())
                .subscribe();
    }

}
