package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
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

import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
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

    //  10 minutes in milliseconds
    @Scheduled(fixedDelay = 600000, initialDelay = 5000)
    void scaricamentoEsitiPec() {

//      Chiamata al servizio imap bridge getMessages per il recupero di tutti i messaggi non letti.
        var getMessages = new GetMessages();
//      Commentato in fase di test per prendere tutte le mail getMessages.setUnseen(1);
        arubaCall.getMessages(getMessages)

//               Lista di byte array. Ognuno di loro rappresenta l'id di un messaggio PEC
                 .flatMap(optionalGetMessagesResponse -> Mono.justOrEmpty(optionalGetMessagesResponse.getArrayOfMessages()))

//               Conversione a Flux di byte[]
                 .flatMapIterable(MesArrayOfMessages::getItem)

//               Conversione a stringa
                 .map(String::new)

                 .doOnNext(msgId -> log.info("Processing PEC with id {}", msgId))

//               Per ogni messaggio trovato, chiamata a getAttach per il download di daticert.xml
                 .flatMap(msgId -> {
                     var getAttach = new GetAttach();
                     getAttach.setMailid(msgId);
                     getAttach.setNameattach("daticert.xml");
                     log.info("Try to download PEC {} daticert.xml", msgId);
                     return arubaCall.getAttach(getAttach).flatMap(getAttachResponse -> {
                         var attachBytes = getAttachResponse.getAttach();

//                       Check se daticert.xml è presente controllando la lunghezza del byte[]
                         if (attachBytes != null && attachBytes.length > 0) {
                             log.info("PEC {} has daticert.xml", msgId);

//                           Deserialize daticert.xml. Start a new Mono inside the flatMap
                             return Mono.fromCallable(() -> daticertService.getPostacertFromByteArray(getAttachResponse.getAttach()))

//                                      Chiamata al gestore repository di EC tramite un messageId PEC. Zip the result with the previous Mono
                                        .zipWhen(postacert -> gestoreRepositoryCall.getRequestByMessageId(postacert.getDati().getMsgid()))

//                                      Preparazione payload per la coda stati PEC
                                        .map(objects -> {
                                            var postacert = objects.getT1();
                                            var requestDto = objects.getT2();
                                            var eventTimestamp = createTimestampFromDaticertDate(postacert.getDati().getData());
                                            var currentStatus = requestDto.getStatusRequest();
                                            var nextStatus = decodePecStatusToMachineStateStatus(postacert.getTipo());
                                            // TODO: EVENT DETAILS CORRETTO ?
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

//                                      Return un Mono contenente il msgId
                                        .thenReturn(msgId);
                         } else {
                             log.info("PEC {} doesn't have daticert.xml", msgId);

//                           Return un Mono contenente il msgId
                             return Mono.just(msgId);
                         }
                     });
                 })

//              Chiamare getMessageID con markseen a uno per marcare il messaggio come letto e terminare il processo.
                 .flatMap(msgId -> {
                     var getMessageID = new GetMessageID();
                     getMessageID.setMailid(msgId);
                     getMessageID.setMarkseen(1);
                     return arubaCall.getMessageId(getMessageID);
                 })

                 .onErrorContinue((throwable, object) -> log.error(throwable.getMessage(), throwable))

                 // TODO: LA STRATEGIA DI SOTTOSCRIZIONE POTREBBE ESSERE PIÙ PERFORMANTE
                 .subscribe();
    }
}
