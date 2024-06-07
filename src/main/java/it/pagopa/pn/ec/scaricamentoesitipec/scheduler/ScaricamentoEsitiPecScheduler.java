package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.library.pec.model.IPostacert;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecListOfMessages;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecMessage;
import it.pagopa.pn.library.pec.model.pojo.PnPostacert;
import it.pagopa.pn.library.pec.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.ScaricamentoEsitiPecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.*;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;

@Component
@CustomLog
public class ScaricamentoEsitiPecScheduler {
    private final DaticertService daticertService;
    private final SqsService sqsService;
    private final ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties;
    private final PnEcPecService pnPecService;
    @Value("${scaricamento-esiti-pec.limit-rate}")
    private Integer limitRate;
    @Value("${pn.ec.storage.sqs.messages.staging.bucket}")
    private String storageSqsMessagesStagingBucket;

    public ScaricamentoEsitiPecScheduler(DaticertService daticertService, SqsService sqsService, ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties, PnEcPecService pnPecService) {
        this.daticertService = daticertService;
        this.sqsService = sqsService;
        this.scaricamentoEsitiPecProperties = scaricamentoEsitiPecProperties;
        this.pnPecService = pnPecService;
    }

    private final Predicate<IPostacert> isPostaCertificataPredicate = postacert -> postacert.getTipo().equals(POSTA_CERTIFICATA);
    private final Predicate<IPostacert> endsWithDomainPredicate = postacert -> postacert.getDati().getMsgid().endsWith(DOMAIN);
    private final Predicate<PnEcPecListOfMessages> hasNoMessages = pnEcPecListOfMessages -> Objects.isNull(pnEcPecListOfMessages) || Objects.isNull(pnEcPecListOfMessages.getMessages()) || pnEcPecListOfMessages.getMessages().isEmpty();

    @Scheduled(cron = "${PnEcCronScaricamentoEsitiPec ?:0 */5 * * * *}")
    public void scaricamentoEsitiPecScheduler() {
        MDC.clear();
        log.logStartingProcess(SCARICAMENTO_ESITI_PEC);
        ScaricamentoEsitiPecUtils.sleepRandomSeconds();
        AtomicReference<String> getUnreadMessagesUUID = new AtomicReference<>();
        AtomicBoolean hasMessages = new AtomicBoolean();
        hasMessages.set(true);

        pnPecService.getMessageCount()
                .then(Mono.defer(() -> pnPecService.getUnreadMessages(Integer.parseInt(scaricamentoEsitiPecProperties.getMessagesLimit()))))
                .flatMap(pnGetMessagesResponse -> {
                    var listOfMessages = pnGetMessagesResponse.getPnEcPecListOfMessages();
                    if (hasNoMessages.test(listOfMessages))
                        hasMessages.set(false);
                    return Mono.justOrEmpty(listOfMessages);
                })
                .flatMapMany(pnEcPecListOfMessages -> {
                    getUnreadMessagesUUID.set(UUID.randomUUID().toString());
                    return Flux.fromIterable(pnEcPecListOfMessages.getMessages());
                })
                .flatMap(pnEcPecMessage -> lavorazioneEsito(pnEcPecMessage, getUnreadMessagesUUID.get()), limitRate)
                .doOnError(throwable -> log.fatal(SCARICAMENTO_ESITI_PEC, throwable))
                .onErrorResume(throwable -> Mono.empty())
                .repeat(hasMessages::get)
                .doOnComplete(() -> log.logEndingProcess(SCARICAMENTO_ESITI_PEC))
                .blockLast();

    }

    public Mono<Void> lavorazioneEsito(PnEcPecMessage pecMessage, String getUnreadMessagesUUID) {

        byte[] message = pecMessage.getMessage();
        String providerName = pecMessage.getProviderName();
        var mimeMessage = getMimeMessage(message);
        var messageID = getMessageIdFromMimeMessage(mimeMessage);

        // TODO: rimuovere una volta verificata assenza di dati sensibili
        log.debug("Message headers: {}", EmailUtils.getHeaders(mimeMessage));

        // controllo del message id e log degli header per reperimento alternativo del messaggio
        if (messageID == null) {
            log.warn("MessageID is null, message headers: {}", EmailUtils.getHeaders(mimeMessage));
            return Mono.empty();
        }
        //Rimozione delle parentesi angolari dal messageID
        if (messageID.startsWith("<") && messageID.endsWith(">"))
            messageID = messageID.substring(1, messageID.length() - 1);
        MDC.put(MDC_CORR_ID_KEY, concatIds(messageID, getUnreadMessagesUUID));
        var finalMessageID = messageID;

        return MDCUtils.addMDCToContextAndExecute(Mono.defer(() -> {

            var attachBytes = getAttachmentFromMimeMessage(mimeMessage, "daticert.xml");

            log.debug(SCARICAMENTO_ESITI_PEC + " - Try to download PEC '{}' daticert.xml", finalMessageID);

//                  Check se daticert.xml è presente controllando la lunghezza del byte[]
            if (!Objects.isNull(attachBytes) && attachBytes.length > 0) {

                log.debug(SCARICAMENTO_ESITI_PEC + " - PEC '{}' has daticert.xml", finalMessageID);

//                      Deserialize daticert.xml. Start a new Mono inside the flatMap
                return Mono.fromCallable(() -> daticertService.getPostacertFromByteArray(attachBytes))
//                                 Escludere questi daticert. Non sono delle 'comunicazione esiti'
                        .filter(isPostaCertificataPredicate.negate())

//                                 msgid arriva all'interno di due angolari <msgid>. Eliminare il primo e l'ultimo carattere
                        .map(postacert -> {
                            var dati = postacert.getDati();
                            var msgId = dati.getMsgid();
                            dati.setMsgid(removeBracketsFromMessageId(msgId));
                            log.debug(SCARICAMENTO_ESITI_PEC + "- PEC '{}' has '{}' msgId", finalMessageID, msgId);
                            return postacert;
                        })

//                               Escludere questi daticert. Non avendo il msgid terminante con il dominio pago non sono state inviate
//                               da noi
                        .filter(endsWithDomainPredicate)

//                               Daticert filtrati
                        .doOnDiscard(PnPostacert.class, postacert -> {
                            if (isPostaCertificataPredicate.test(postacert)) {
                                log.debug(PEC_DISCARDED, finalMessageID, SCARICAMENTO_ESITI_PEC, POSTA_CERTIFICATA);
                            } else if (!endsWithDomainPredicate.test(postacert)) {
                                log.debug(PEC_DISCARDED, finalMessageID, SCARICAMENTO_ESITI_PEC, NOT_SENT_BY_US);
                            }
                        })
                        .flatMap(unused -> sqsService.sendWithLargePayload(scaricamentoEsitiPecProperties.sqsQueueName(),
                                finalMessageID,
                                storageSqsMessagesStagingBucket,
                                RicezioneEsitiPecDto.builder()
                                        .messageID(finalMessageID)
                                        .message(message)
                                        .receiversDomain(getDomainFromAddress(getFromFromMimeMessage(mimeMessage)[0]))
                                        .retry(0)
                                        .build()))
                        .thenReturn(Tuples.of(finalMessageID, providerName));
            } else return Mono.just(Tuples.of(finalMessageID, providerName));
        })
         //Marca il messaggio come letto.
         .flatMap(tuple -> pnPecService.markMessageAsRead(tuple.getT1(), tuple.getT2())));
    }
}
