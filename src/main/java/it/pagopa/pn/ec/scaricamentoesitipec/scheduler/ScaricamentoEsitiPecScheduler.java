package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.library.pec.model.pojo.IPostacert;
import it.pagopa.pn.library.pec.pojo.PnPostacert;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.library.pec.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.ScaricamentoEsitiPecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pec.bridgews.*;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.DOMAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;

@Component
@CustomLog
public class ScaricamentoEsitiPecScheduler {

    private final ArubaCall arubaCall;
    private final DaticertService daticertService;
    private final SqsService sqsService;
    private final ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties;
    private final CloudWatchPecMetrics cloudWatchPecMetrics;
    private final PnPecService pnPecService;
    @Value("${scaricamento-esiti-pec.limit-rate}")
    private Integer limitRate;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall, DaticertService daticertService, SqsService sqsService, ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties, CloudWatchPecMetrics cloudWatchPecMetrics, PnPecService pnPecService) {
        this.arubaCall = arubaCall;
        this.daticertService = daticertService;
        this.sqsService = sqsService;
        this.scaricamentoEsitiPecProperties = scaricamentoEsitiPecProperties;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.pnPecService = pnPecService;
    }

    private final Predicate<IPostacert> isPostaCertificataPredicate = postacert -> postacert.getTipo().equals(POSTA_CERTIFICATA);
    private final Predicate<IPostacert> endsWithDomainPredicate = postacert -> postacert.getDati().getMsgid().endsWith(DOMAIN);

    private GetMessageID createGetMessageIdRequest(String messageID, Integer isuid, boolean markSeen) {
        var getMessageID = new GetMessageID();
        getMessageID.setMailid(messageID);
        getMessageID.setIsuid(isuid);
        getMessageID.setMarkseen(markSeen ? 1 : 0);
        return getMessageID;
    }

    @Scheduled(cron = "${PnEcCronScaricamentoEsitiPec ?:0 */5 * * * *}")
    public void scaricamentoEsitiPecScheduler() {

        log.logStartingProcess(SCARICAMENTO_ESITI_PEC);
        ScaricamentoEsitiPecUtils.sleepRandomSeconds();

        var getMessages = new GetMessages();
        getMessages.setUnseen(1);
        getMessages.setOuttype(2);
        getMessages.setLimit(Integer.valueOf(scaricamentoEsitiPecProperties.getMessagesLimit()));

        AtomicBoolean hasMessages = new AtomicBoolean();
        hasMessages.set(true);

        MDCUtils.addMDCToContextAndExecute(pnPecService.getMessageCount()
                .flatMap(messageCount -> cloudWatchPecMetrics.publishMessageCount((long) messageCount))
                .then(arubaCall.getMessages(getMessages))
                .flatMap(getMessagesResponse -> {
                    var arrayOfMessages = getMessagesResponse.getArrayOfMessages();
                    if (Objects.isNull(arrayOfMessages))
                        hasMessages.set(false);
                    return Mono.justOrEmpty(arrayOfMessages);
                })
                .flatMapIterable(MesArrayOfMessages::getItem)
                .flatMap(message -> {

                    var mimeMessage = getMimeMessage(message);
                    var messageID = getMessageIdFromMimeMessage(mimeMessage);
                    MDC.put(MDC_CORR_ID_KEY, messageID);
                    //Rimozione delle parentesi angolari dal messageID
                    var finalMessageID = messageID.substring(1, messageID.length() - 1);
                    var attachBytes = findAttachmentByName(mimeMessage, "daticert.xml");

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
                                    dati.setMsgid(msgId.substring(1, msgId.length() - 1));
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
                                        log.debug(PEC_DISCARDED,finalMessageID, SCARICAMENTO_ESITI_PEC, NOT_SENT_BY_US);
                                    }
                                })
                                 .flatMap(unused -> sqsService.send(scaricamentoEsitiPecProperties.sqsQueueName(), finalMessageID, RicezioneEsitiPecDto.builder()
                                         .messageID(finalMessageID)
                                         .message(message)
                                         .receiversDomain(getDomainFromAddress(getFromFromMimeMessage(mimeMessage)[0]))
                                         .retry(0)
                                         .build()))
                                 .thenReturn(finalMessageID);
                    }
                    else return Mono.just(finalMessageID);
                })
                //Marca il messaggio come letto.
                .flatMap(finalMessageID -> arubaCall.getMessageId(createGetMessageIdRequest(finalMessageID, 2, true)), limitRate)
                .doOnError(throwable -> log.fatal(SCARICAMENTO_ESITI_PEC, throwable))
                .onErrorResume(throwable -> Mono.empty())
                .repeat(hasMessages::get)
                .doOnComplete(() -> log.logEndingProcess(SCARICAMENTO_ESITI_PEC)))
                .subscribe();

    }

}
