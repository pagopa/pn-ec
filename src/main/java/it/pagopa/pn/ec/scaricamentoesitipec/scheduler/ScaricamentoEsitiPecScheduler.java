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
import it.pec.bridgews.GetMessages;
import it.pec.bridgews.GetMessagesResponse;
import it.pec.bridgews.MesArrayOfMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.decodePecStatusToMachineStateStatus;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@EnableScheduling
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

    @Scheduled(fixedDelay = 10000)
    void scaricamentoEsitiPec() {

//      Chiamata al servizio imap bridge getMessages per il recupero di tutti i messaggi non letti.
        var getMessages = new GetMessages();
//        getMessages.setUnseen(1);
        arubaCall.getMessages(getMessages)

//               Lista di byte array. Ognuno di loro rappresenta l'id di un messaggio PEC
                 .map(GetMessagesResponse::getArrayOfMessages)

//               Conversione a Flux di byte[]
                 .flatMapIterable(MesArrayOfMessages::getItem)

//               Conversione a stringa
                 .map(idInBytes -> new String(idInBytes, UTF_8))

                 .doOnNext(msgId -> log.info("Processing PEC with id {}", msgId))

//               Per ogni messaggio trovato, chiamata a getAttach per il download di daticert.xml
                 .flatMap(msgId -> {
                     var getAttach = new GetAttach();
                     getAttach.setMailid(msgId);
                     getAttach.setNameattach("daticert.xml");
                     return arubaCall.getAttach(getAttach);
                 })

//               Deserialize daticert.xml
                 .map(getAttachResponse -> daticertService.getPostacertFromByteArray(getAttachResponse.getAttach()))

//               Chiamata al gestore repository di EC tramite un messageId PEC
                 .flatMap(postacert -> Mono.zip(Mono.just(postacert),
                                                gestoreRepositoryCall.getRequestByMessageId(postacert.getDati().getMsgid()),
                                                Mono.just(decodePecStatusToMachineStateStatus(postacert.getTipo()))))

//               Pubblicazione sulla coda degli stati PEC
                 .flatMap(objects -> {
                     var postacert = objects.getT1();
                     var requestDto = objects.getT2();

                     var requestIdx = requestDto.getRequestIdx();
                     var xPagopaExtchCxId = requestDto.getxPagopaExtchCxId();
                     var eventTimestamp = createTimestampFromDaticertDate(postacert.getDati().getData());
                     var processId = transactionProcessConfigurationProperties.pecStartStatus();
                     var currentStatus = requestDto.getStatusRequest();
                     var nextStatus = objects.getT3();
                     // TODO: EVENT DETAILS CORRETTO ?
                     var eventDetails = postacert.getErrore();
                     // TODO: COME RECUPERARE SYSTEM E LOCATION ?
                     var generatedMessageDto = new GeneratedMessageDto().id(postacert.getDati().getMsgid());

                     return sqsService.send(notificationTrackerSqsName.statoPecName(),
                                            new NotificationTrackerQueueDto(requestIdx,
                                                                            xPagopaExtchCxId,
                                                                            eventTimestamp,
                                                                            processId,
                                                                            currentStatus,
                                                                            nextStatus,
                                                                            eventDetails,
                                                                            generatedMessageDto));
                 })

                 .subscribe();
    }
}
