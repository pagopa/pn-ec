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
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchPecMetricsInfo;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pec.bridgews.*;
import it.pec.daticert.Postacert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Random;
import java.util.function.Predicate;

import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.DOMAIN;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.decodeMessageId;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.createGeneratedMessageByStatus;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.decodePecStatusToMachineStateStatus;

@Component
@Slf4j
public class ScaricamentoEsitiPecScheduler {

    private final ArubaCall arubaCall;
    private final DaticertService daticertService;
    private final CallMacchinaStati callMacchinaStati;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final StatusPullService statusPullService;
    private final SqsService sqsService;
    private final CloudWatchPecMetrics cloudWatchPecMetrics;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final ArubaSecretValue arubaSecretValue;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    private final Random random;

    @Value("${scaricamento-esiti-pec.get-messages.limit}")
    private String scaricamentoEsitiPecGetMessagesLimit;

    @Value("${scaricamento-esiti-pec.maximum-delay-seconds}")
    private String maximumDelaySeconds;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall, DaticertService daticertService, CallMacchinaStati callMacchinaStati,
                                         GestoreRepositoryCall gestoreRepositoryCall, StatusPullService statusPullService,
                                         SqsService sqsService, CloudWatchPecMetrics cloudWatchPecMetrics,
                                         NotificationTrackerSqsName notificationTrackerSqsName, ArubaSecretValue arubaSecretValue,
                                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties,
                                         Random random) {
        this.arubaCall = arubaCall;
        this.daticertService = daticertService;
        this.callMacchinaStati = callMacchinaStati;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.statusPullService = statusPullService;
        this.sqsService = sqsService;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.arubaSecretValue = arubaSecretValue;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
        this.random = random;
    }

    private final Predicate<Postacert> isPostaCertificataPredicate = postacert -> postacert.getTipo().equals(POSTA_CERTIFICATA);
    private final Predicate<Postacert> endsWithDomainPredicate = postacert -> postacert.getDati().getMsgid().endsWith(DOMAIN);
    private boolean isScaricamentoEsitiPecRunning = false;

    private GetMessageID createGetMessageIdRequest(String pecId, boolean markSeen) {
        var getMessageID = new GetMessageID();
        getMessageID.setMailid(pecId);
        getMessageID.setIsuid(1);
        getMessageID.setMarkseen(markSeen ? 1 : null);
        return getMessageID;
    }

    @Scheduled(cron = "${cron.value.scaricamento-esiti-pec}")
    void scaricamentoEsitiPec() {

        log.info("<-- SCARICAMENTO ESITI PEC SCHEDULER -->");

//      Prevent scaricamentoEsitiPec overlapping
        Mono.just(isScaricamentoEsitiPecRunning)
            .filter(aBoolean -> !aBoolean)
            .doOnNext(aBoolean -> isScaricamentoEsitiPecRunning = true)

//          Since this scheduled method could be launched simultaneously in multiple instances of the microservice, a random delay was
//          added to avoid processing the same information multiple times
            .delayElement(Duration.ofSeconds(random.nextInt(Integer.parseInt(maximumDelaySeconds) + 1)))

//          Chiamata al servizio imap bridge getMessages per il recupero di tutti i messaggi non letti.
            .flatMap(aBoolean -> {
                var getMessages = new GetMessages();
                getMessages.setUnseen(1);
                getMessages.setLimit(Integer.valueOf(scaricamentoEsitiPecGetMessagesLimit));
                return arubaCall.getMessages(getMessages);
            })
            .doOnError(ArubaCallMaxRetriesExceededException.class, e -> log.debug("Aruba non risponde. Circuit breaker"))
            .onErrorComplete(ArubaCallMaxRetriesExceededException.class)

//          Lista di byte array. Ognuno di loro rappresenta l'id di un messaggio PEC
            .flatMap(optionalGetMessagesResponse -> Mono.justOrEmpty(optionalGetMessagesResponse.getArrayOfMessages()))

            .doOnNext(mesArrayOfMessages -> log.debug("Retrieved {} unseen PEC", mesArrayOfMessages.getItem().size()))

//          Conversione a Flux di byte[]
            .flatMapIterable(MesArrayOfMessages::getItem)

//          Conversione a stringa
            .map(String::new)

            .doOnNext(pecId -> log.debug("Processing PEC with id {}", pecId))

//          Per ogni messaggio trovato, chiamata a getAttach per il download di daticert.xml
            .flatMap(pecId -> {
                var getAttach = new GetAttach();
                getAttach.setMailid(pecId);
                getAttach.setNameattach("daticert.xml");

                log.debug("Try to download PEC {} daticert.xml", pecId);

                return arubaCall.getAttach(getAttach).flatMap(getAttachResponse -> {
                    var attachBytes = getAttachResponse.getAttach();

//                  Check se daticert.xml è presente controllando la lunghezza del byte[]
                    if (attachBytes != null && attachBytes.length > 0) {
                        log.debug("PEC {} has daticert.xml", pecId);

//                      Deserialize daticert.xml. Start a new Mono inside the flatMap
                        return Mono.fromCallable(() -> daticertService.getPostacertFromByteArray(getAttachResponse.getAttach()))

//                                 Escludere queste PEC. Non sono delle 'comunicazione esiti'
                                   .filter(isPostaCertificataPredicate.negate())

//                                 msgid arriva all'interno di due angolari <msgid>. Eliminare il primo e l'ultimo carattere
                                   .map(postacert -> {
                                       var dati = postacert.getDati();
                                       var msgId = dati.getMsgid();
                                       dati.setMsgid(msgId.substring(1, msgId.length() - 1));
                                       log.debug("PEC {} has {} msgId", pecId, msgId);
                                       return postacert;
                                   })

//                                 Escludere queste PEC. Non avendo il msgid terminante con il dominio pago non sono state inviate da noi
                                   .filter(endsWithDomainPredicate)

                                   .doOnDiscard(Postacert.class, postacert -> {
                                       if (isPostaCertificataPredicate.test(postacert)) {
                                           log.debug("PEC {} discarded, is {}", pecId, POSTA_CERTIFICATA);
                                       } else if (!endsWithDomainPredicate.test(postacert)) {
                                           log.debug("PEC {} discarded, it was not sent by us", pecId);
                                       }
                                   })

                                   .flatMap(postacert -> {
                                       var presaInCaricoInfo = decodeMessageId(postacert.getDati().getMsgid());
                                       var requestIdx = presaInCaricoInfo.getRequestIdx();

                                       return Mono.zip(Mono.just(postacert),
                                                       gestoreRepositoryCall.getRichiesta(requestIdx),
                                                       statusPullService.pecPullService(requestIdx,
                                                                                        presaInCaricoInfo.getXPagopaExtchCxId()),
                                                       arubaCall.getMessageId(createGetMessageIdRequest(pecId, false)));
                                   })

                                   .flatMap(objects -> {
                                       Postacert postacert = objects.getT1();
                                       RequestDto requestDto = objects.getT2();
                                       LegalMessageSentDetails legalMessageSentDetails = objects.getT3();
                                       GetMessageIDResponse getMessageIDResponse = objects.getT4();

                                       var nextStatus =
                                               decodePecStatusToMachineStateStatus(postacert.getTipo()).getStatusTransactionTableCompliant();
                                       var nextEventTimestamp = createTimestampFromDaticertDate(postacert.getDati().getData());
                                       var cloudWatchPecMetricsInfo = CloudWatchPecMetricsInfo.builder()
                                                                                              .previousStatus(requestDto.getStatusRequest())
                                                                                              .previousEventTimestamp(
                                                                                                      legalMessageSentDetails.getEventTimestamp())
                                                                                              .nextStatus(nextStatus)
                                                                                              .nextEventTimestamp(nextEventTimestamp)
                                                                                              .build();

                                       return cloudWatchPecMetrics.publishCustomPecMetrics(cloudWatchPecMetricsInfo)
                                                                  .thenReturn(Tuples.of(postacert,
                                                                                        getMessageIDResponse,
                                                                                        requestDto,
                                                                                        cloudWatchPecMetricsInfo));
                                   })

//                                 Validate status
                                   .flatMap(objects -> {
                                       var postacert = objects.getT1();
                                       var getMessageIDResponse = objects.getT2();
                                       var requestDto = objects.getT3();
                                       var cloudWatchPecMetricsInfo = objects.getT4();

                                       var requestStatusChange = RequestStatusChange.builder()
                                                                                    .requestIdx(requestDto.getRequestIdx())
                                                                                    .xPagopaExtchCxId(requestDto.getxPagopaExtchCxId())
                                                                                    .processId(transactionProcessConfigurationProperties.pec())
                                                                                    .nextStatus(cloudWatchPecMetricsInfo.getNextStatus())
                                                                                    .currentStatus(requestDto.getStatusRequest())
                                                                                    .build();

                                       return callMacchinaStati.statusValidation(requestStatusChange)
                                                               .map(unused -> Tuples.of(postacert,
                                                                                        getMessageIDResponse,
                                                                                        requestStatusChange,
                                                                                        cloudWatchPecMetricsInfo))
                                                               .doOnError(CallMacchinaStati.StatusValidationBadRequestException.class,
                                                                          throwable -> log.debug(
                                                                                  "La chiamata al notification tracker della PEC {} " +
                                                                                  "associata alla richiesta {} ha tornato 400 come " +
                                                                                  "status",
                                                                                  pecId,
                                                                                  requestDto.getRequestIdx()))
                                                               .doOnError(InvalidNextStatusException.class,
                                                                          throwable -> log.debug(
                                                                                  "La PEC {} associata alla richiesta {} ha " +
                                                                                  "comunicato i propri" + " esiti in " +
                                                                                  "un ordine non corretto al notification tracker",
                                                                                  pecId,
                                                                                  requestDto.getRequestIdx()));
                                   })

//                                 Preparazione payload per la coda stati PEC
                                   .map(objects -> {
                                       Postacert postacert = objects.getT1();
                                       GetMessageIDResponse getMessageIDResponse = objects.getT2();
                                       RequestStatusChange requestStatusChange = objects.getT3();
                                       CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo = objects.getT4();

                                       var mimeMessage = getMimeMessage(getMessageIDResponse.getMessage());
                                       var pecIdMessageId = getMessageIdFromMimeMessage(mimeMessage);
                                       var requestIdx = requestStatusChange.getRequestIdx();
                                       var xPagopaExtchCxId = requestStatusChange.getXPagopaExtchCxId();
                                       var currentStatus = requestStatusChange.getCurrentStatus();
                                       var nextStatus = requestStatusChange.getNextStatus();
                                       var eventDetails = postacert.getErrore();
                                       var senderDigitalAddress = arubaSecretValue.getPecUsername();
                                       var senderDomain = getDomainFromAddress(senderDigitalAddress);
                                       var receiversDomain = getDomainFromAddress(getFromFromMimeMessage(mimeMessage)[0]);
                                       var generatedMessageDto = createGeneratedMessageByStatus(receiversDomain,
                                                                                                senderDomain,
                                                                                                pecIdMessageId,
                                                                                                postacert.getTipo(),
                                                                                                // TODO: COME RECUPERARE LOCATION ?
                                                                                                null);

                                       log.debug("PEC {} has {} requestId", pecId, requestIdx);


                                       var digitalProgressStatusDto =
                                               new DigitalProgressStatusDto().eventTimestamp(cloudWatchPecMetricsInfo.getNextEventTimestamp())
                                                                             .eventDetails(eventDetails)
                                                                             .generatedMessage(generatedMessageDto);

                                       return NotificationTrackerQueueDto.builder()
                                                                         .requestIdx(requestIdx)
                                                                         .xPagopaExtchCxId(xPagopaExtchCxId)
                                                                         .currentStatus(currentStatus)
                                                                         .nextStatus(nextStatus)
                                                                         .digitalProgressStatusDto(digitalProgressStatusDto)
                                                                         .build();
                                   })

//                                 Pubblicazione sulla coda degli stati PEC
                                   .flatMap(notificationTrackerQueueDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                                           notificationTrackerQueueDto))

//                                 Return un Mono contenente il pecId
                                   .thenReturn(pecId)

//                                 Se per qualche motivo questo daticert è da escludere tornare comunque il pecId
                                   .switchIfEmpty(Mono.just(pecId));
                    } else {
                        log.debug("PEC {} doesn't have daticert.xml", pecId);

//                      Return un Mono contenente il pecId
                        return Mono.just(pecId);
                    }
                });
            })

//          Chiamare getMessageID con markseen a uno per marcare il messaggio come letto e terminare il processo.
            .flatMap(pecId -> arubaCall.getMessageId(createGetMessageIdRequest(pecId, true))
                                       .doOnSuccess(getMessageIDResponse -> log.debug("PEC {} marked as seen", pecId)))

//          Se avviene qualche errore per una particolare PEC non bloccare il Flux
            .onErrorContinue(CallMacchinaStati.StatusValidationBadRequestException.class,
                             (throwable, o) -> log.debug(throwable.getMessage()))
            .onErrorContinue(InvalidNextStatusException.class, (throwable, o) -> log.debug(throwable.getMessage()))
            .onErrorContinue((throwable, object) -> log.error(throwable.getMessage(), throwable))

            .doOnComplete(() -> isScaricamentoEsitiPecRunning = false)

            .subscribe();
    }
}
