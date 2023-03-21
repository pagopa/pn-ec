package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.request.RequestStatusChange;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pec.bridgews.*;
import it.pec.daticert.Postacert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.DOMAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.createGeneratedMessageByStatus;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.decodePecStatusToMachineStateStatus;

@Component
@Slf4j
public class ScaricamentoEsitiPecScheduler {

    private final ArubaCall arubaCall;
    private final DaticertService daticertService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMacchinaStati;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final ArubaSecretValue arubaSecretValue;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall, DaticertService daticertService,
                                         GestoreRepositoryCall gestoreRepositoryCall, CallMacchinaStati callMacchinaStati,
                                         SqsService sqsService, NotificationTrackerSqsName notificationTrackerSqsName,
                                         ArubaSecretValue arubaSecretValue,
                                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.arubaCall = arubaCall;
        this.daticertService = daticertService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMacchinaStati = callMacchinaStati;
        this.sqsService = sqsService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.arubaSecretValue = arubaSecretValue;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Scheduled(cron = "${cron.value.scaricamento-esiti-pec}")
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

                 .doOnNext(mesArrayOfMessages -> log.info("Retrieved {} unseen PEC", mesArrayOfMessages.getItem().size()))

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

//                                      Escludere queste PEC. Non sono delle 'comunicazione esiti'
                                        .filter(postacert -> !postacert.getTipo().equals(POSTA_CERTIFICATA))
                                        .doOnDiscard(Postacert.class,
                                                     postacert -> log.info("PEC {} discarded, is {}", pecId, POSTA_CERTIFICATA))

//                                      msgid arriva all'interno di due angolari <msgid>. Eliminare il primo e l'ultimo carattere
                                        .map(postacert -> {
                                            var dati = postacert.getDati();
                                            var msgId = dati.getMsgid();
                                            dati.setMsgid(msgId.substring(1, msgId.length() - 1));
                                            log.info("PEC {} has {} msgId", pecId, msgId);
                                            return postacert;
                                        })

//                                      Escludere queste PEC. Non avendo il msgid terminante con il dominio pago non sono state inviate
//                                      da noi
                                        .filter(postacert -> postacert.getDati().getMsgid().endsWith(DOMAIN))

//                                      Chiamata al gestore repository di EC tramite un messageId PEC. Zip the result with the previous
//                                      Mono<Postacert>
                                        .flatMap(postacert -> {
                                            var getMessageID = new GetMessageID();
                                            getMessageID.setMailid(pecId);
                                            return Mono.zip(Mono.just(postacert),
                                                            arubaCall.getMessageId(getMessageID),
                                                            gestoreRepositoryCall.getRequestByMessageId(postacert.getDati().getMsgid()));
                                        })

//                                      Validate status
                                        .flatMap(objects -> {
                                            var postacert = objects.getT1();
                                            var getMessageIDResponse = objects.getT2();
                                            var requestDto = objects.getT3();
                                            var requestStatusChange = RequestStatusChange.builder()
                                                                                         .requestIdx(requestDto.getRequestIdx())
                                                                                         .xPagopaExtchCxId(requestDto.getxPagopaExtchCxId())
                                                                                         .processId(
                                                                                                 transactionProcessConfigurationProperties.pec())
                                                                                         .nextStatus(decodePecStatusToMachineStateStatus(
                                                                                                 postacert.getTipo()).getStatusTransactionTableCompliant())
                                                                                         .currentStatus(requestDto.getStatusRequest())
                                                                                         .build();
                                            return callMacchinaStati.statusValidation(requestStatusChange)
                                                                    .map(unused -> Tuples.of(postacert,
                                                                                             getMessageIDResponse,
                                                                                             requestStatusChange,
                                                                                             requestDto.getRequestPersonal()
                                                                                                       .getDigitalRequestPersonal()
                                                                                                       .getReceiverDigitalAddress()))
                                                                    .doOnError(throwable -> log.debug(
                                                                            "La PEC {} associata alla richiesta {} ha " +
                                                                            "comunicato i propri" + " esiti in " +
                                                                            "un ordine non corretto al notification tracker",
                                                                            pecId,
                                                                            requestDto.getRequestIdx()));
                                        })

//                                      Preparazione payload per la coda stati PEC
                                        .map(objects -> {
                                            Postacert postacert = objects.getT1();
                                            GetMessageIDResponse getMessageIDResponse = objects.getT2();
                                            RequestStatusChange requestStatusChange = objects.getT3();
                                            String receiverDigitalAddress = objects.getT4();
                                            var pecIdMessageId =
                                                    getMessageIdFromMimeMessage(getMimeMessage(getMessageIDResponse.getMessage()));
                                            var requestIdx = requestStatusChange.getRequestIdx();
                                            var xPagopaExtchCxId = requestStatusChange.getXPagopaExtchCxId();
                                            var currentStatus = requestStatusChange.getCurrentStatus();
                                            var nextStatus = requestStatusChange.getNextStatus();
                                            var postacertDati = postacert.getDati();
                                            var eventTimestamp = createTimestampFromDaticertDate(postacertDati.getData());
                                            var eventDetails = postacert.getErrore();
                                            var senderDigitalAddress = arubaSecretValue.getPecUsername();
                                            var senderDomain = getDomainFromAddress(senderDigitalAddress);
                                            var receiverDomain = getDomainFromAddress(receiverDigitalAddress);
                                            var generatedMessageDto = createGeneratedMessageByStatus(receiverDomain,
                                                                                                     senderDomain,
                                                                                                     pecIdMessageId,
                                                                                                     postacert.getTipo(),
                                                                                                     // TODO: COME RECUPERARE LOCATION ?
                                                                                                     null);

                                            log.info("PEC {} has {} requestId", pecId, requestIdx);

                                            return NotificationTrackerQueueDto.builder()
                                                                              .requestIdx(requestIdx)
                                                                              .xPagopaExtchCxId(xPagopaExtchCxId)
                                                                              .currentStatus(currentStatus)
                                                                              .nextStatus(nextStatus)
                                                                              .digitalProgressStatusDto(new DigitalProgressStatusDto().eventTimestamp(
                                                                                                                                              eventTimestamp)
                                                                                                                                      .eventDetails(
                                                                                                                                              eventDetails)
                                                                                                                                      .generatedMessage(
                                                                                                                                              generatedMessageDto))
                                                                              .build();
                                        })

//                                      Pubblicazione sulla coda degli stati PEC
                                        .flatMap(notificationTrackerQueueDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                                                notificationTrackerQueueDto))

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
                     var getMessageID = new GetMessageID();
                     getMessageID.setMailid(pecId);
                     getMessageID.setMarkseen(1);
                     return arubaCall.getMessageId(getMessageID)
                                     .doOnSuccess(getMessageIDResponse -> log.info("PEC {} marked as seen", pecId));
                 })

//               Se avviene qualche errore per una particolare PEC non bloccare il Flux
                 .onErrorContinue(InvalidNextStatusException.class, (throwable, o) -> log.debug(throwable.getMessage()))
                 .onErrorContinue((throwable, object) -> log.error(throwable.getMessage(), throwable))

                 .subscribe();
    }
}
