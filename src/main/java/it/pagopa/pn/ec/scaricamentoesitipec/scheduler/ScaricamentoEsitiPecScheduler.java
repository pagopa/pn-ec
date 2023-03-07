package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pec.bridgews.GetAttach;
import it.pec.bridgews.GetMessageID;
import it.pec.bridgews.GetMessages;
import it.pec.bridgews.MesArrayOfMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.decodePecStatusToMachineStateStatus;

@Component
@Slf4j
public class ScaricamentoEsitiPecScheduler {

    private final ArubaCall arubaCall;
    private final DaticertService daticertService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall, DaticertService daticertService,
                                         GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                                         NotificationTrackerSqsName notificationTrackerSqsName,
                                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.arubaCall = arubaCall;
        this.daticertService = daticertService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.sqsService = sqsService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES)
    void scaricamentoEsitiPec() {

        log.info("<-- SCARICAMENTO ESITI PEC SCHEDULER -->");

//      Chiamata al servizio imap bridge getMessages per il recupero di tutti i messaggi non letti.
        var getMessages = new GetMessages();
        getMessages.setUnseen(1);
        arubaCall.getMessages(getMessages)
                 .doOnError(ArubaCallMaxRetriesExceededException.class, e -> log.error("Aruba non risponde. Circuit breaker"))
                 .onErrorComplete(ArubaCallMaxRetriesExceededException.class)

//               Lista di byte array. Ognuno di loro rappresenta l'id di un messaggio PEC
                 .flatMap(optionalGetMessagesResponse -> Mono.justOrEmpty(optionalGetMessagesResponse.getArrayOfMessages()))

//               Conversione a Flux di byte[]
                 .flatMapIterable(MesArrayOfMessages::getItem)

//               Conversione a stringa
                 .map(String::new)

                 .doOnNext(pecId -> log.info("Processing PEC with id {}", pecId))

//               Per ogni messaggio trovato, chiamata a getAttach per il download di daticert.xml
                 .flatMap(pecId -> {
                     var getAttach = new GetAttach();
                     getAttach.setMailid(pecId);
                     getAttach.setNameattach("daticert.xml");
                     log.info("Try to download PEC {} daticert.xml", pecId);
                     return arubaCall.getAttach(getAttach).flatMap(getAttachResponse -> {
                         var attachBytes = getAttachResponse.getAttach();

//                       Check se daticert.xml è presente controllando la lunghezza del byte[]
                         if (attachBytes != null && attachBytes.length > 0) {
                             log.info("PEC {} has daticert.xml", pecId);

//                           Deserialize daticert.xml. Start a new Mono inside the flatMap
                             return Mono.fromCallable(() -> daticertService.getPostacertFromByteArray(getAttachResponse.getAttach()))

//                                      Escludere questi daticert
                                        .filter(postacert -> !postacert.getTipo().equals(POSTA_CERTIFICATA))

//                                      msgid arriva all'interno di due angolari <msgid>. Eliminare il primo e l'ultimo carattere
                                        .map(postacert -> {
                                            var dati = postacert.getDati();
                                            var msgId = dati.getMsgid();
                                            dati.setMsgid(msgId.substring(1, msgId.length() - 1));
                                            return postacert;
                                        })

//                                      Chiamata al gestore repository di EC tramite un messageId PEC. Zip the result with the previous Mono
                                        .zipWhen(postacert -> gestoreRepositoryCall.getRequestByMessageId(postacert.getDati().getMsgid()))

//                                      Preparazione payload per la coda stati PEC
                                        .map(objects -> {
                                            var postacert = objects.getT1();
                                            var requestDto = objects.getT2();
                                            var eventTimestamp = createTimestampFromDaticertDate(postacert.getDati().getData());
                                            var currentStatus = requestDto.getStatusRequest();
                                            var nextStatus = decodePecStatusToMachineStateStatus(postacert.getTipo());
                                            var eventDetails = postacert.getErrore();
                                            // TODO: COME RECUPERARE SYSTEM E LOCATION ?
                                            var generatedMessageDto = new GeneratedMessageDto().id(postacert.getDati().getMsgid());
                                            return new NotificationTrackerQueueDto(requestDto.getRequestIdx(),
                                                                                   requestDto.getxPagopaExtchCxId(),
                                                                                   eventTimestamp,
                                                                                   transactionProcessConfigurationProperties.pec(),
                                                                                   currentStatus,
                                                                                   nextStatus,
                                                                                   eventDetails,
                                                                                   generatedMessageDto);
                                        })

//                                      Pubblicazione sulla coda degli stati PEC
                                        .flatMap(dto -> sqsService.send(notificationTrackerSqsName.statoPecName(), dto))

//                                      Return un Mono contenente il pecId
                                        .thenReturn(pecId)

//                                      Se per qualche motivo questo daticert è da escludere tornare comunque il pecId
                                        .switchIfEmpty(Mono.just(pecId));
                         } else {
                             log.info("PEC {} doesn't have daticert.xml", pecId);

//                           Return un Mono contenente il pecId
                             return Mono.just(pecId);
                         }
                     });
                 })

//              Chiamare getMessageID con markseen a uno per marcare il messaggio come letto e terminare il processo.
                 .flatMap(pecId -> {
                     log.info("PEC {} marked as seen", pecId);
                     var getMessageID = new GetMessageID();
                     getMessageID.setMailid(pecId);
                     getMessageID.setMarkseen(1);
                     return arubaCall.getMessageId(getMessageID);
                 })

//               Se avviene qualche errore per una particolare PEC non bloccare il Flux
                 .onErrorContinue((throwable, object) -> log.error(throwable.getMessage(), throwable))

                 // TODO: LA STRATEGIA DI SOTTOSCRIZIONE POTREBBE ESSERE PIÙ PERFORMANTE
                 .subscribe();
    }
}
