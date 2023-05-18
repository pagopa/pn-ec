package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.exception.ShaGenerationException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.consolidatore.utils.ContentTypes;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchPecMetricsInfo;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pec.bridgews.*;
import it.pec.daticert.Postacert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;
import java.util.function.Predicate;

import static it.pagopa.pn.ec.commons.constant.DocumentType.PN_EXTERNAL_LEGAL_FACTS;
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
    private final FileCall fileCall;
    private final CloudWatchPecMetrics cloudWatchPecMetrics;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final ArubaSecretValue arubaSecretValue;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    private final Random random;
    private final WebClient uploadWebClient;
    private static final String SAFESTORAGE_PREFIX = "safestorage://";
    @Value("${scaricamento-esiti-pec.get-messages.limit}")
    private String scaricamentoEsitiPecGetMessagesLimit;

    @Value("${scaricamento-esiti-pec.maximum-delay-seconds}")
    private String maximumDelaySeconds;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall, DaticertService daticertService, CallMacchinaStati callMacchinaStati,
                                         GestoreRepositoryCall gestoreRepositoryCall, StatusPullService statusPullService, SqsService sqsService, FileCall fileCall, CloudWatchPecMetrics cloudWatchPecMetrics,
                                         NotificationTrackerSqsName notificationTrackerSqsName, ArubaSecretValue arubaSecretValue,
                                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties,
                                         Random random, WebClient uploadWebClient) {
        this.arubaCall = arubaCall;
        this.daticertService = daticertService;
        this.callMacchinaStati = callMacchinaStati;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.statusPullService = statusPullService;
        this.sqsService = sqsService;
        this.fileCall = fileCall;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.arubaSecretValue = arubaSecretValue;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
        this.random = random;
        this.uploadWebClient = uploadWebClient;
    }

    private final Predicate<Postacert> isPostaCertificataPredicate = postacert -> postacert.getTipo().equals(POSTA_CERTIFICATA);
    private final Predicate<Postacert> endsWithDomainPredicate = postacert -> postacert.getDati().getMsgid().endsWith(DOMAIN);
    private boolean isScaricamentoEsitiPecRunning = false;

    private GetMessageID createGetMessageIdRequest(String pecId, boolean markSeen) {
        var getMessageID = new GetMessageID();
        getMessageID.setMailid(pecId);
        getMessageID.setIsuid(1);
        getMessageID.setMarkseen(markSeen ? 1 : 0);
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
                    getAttach.setIsuid(1);
                    getAttach.setMarkseen(0);
                    getAttach.setNameattach("daticert.xml");

                    log.debug("Try to download PEC {} daticert.xml", pecId);

                    return arubaCall.getAttach(getAttach).flatMap(getAttachResponse -> {
                        var attachBytes = getAttachResponse.getAttach();

//                  Check se daticert.xml è presente controllando la lunghezza del byte[]
                        if (attachBytes != null && attachBytes.length > 0) {
                            log.debug("PEC {} has daticert.xml", pecId);

//                      Deserialize daticert.xml. Start a new Mono inside the flatMap
                            return Mono.fromCallable(() -> daticertService.getPostacertFromByteArray(getAttachResponse.getAttach()))

//                                 Escludere questi daticert. Non sono delle 'comunicazione esiti'
                                    .filter(isPostaCertificataPredicate.negate())

//                                 msgid arriva all'interno di due angolari <msgid>. Eliminare il primo e l'ultimo carattere
                                    .map(postacert -> {
                                        var dati = postacert.getDati();
                                        var msgId = dati.getMsgid();
                                        dati.setMsgid(msgId.substring(1, msgId.length() - 1));
                                        log.debug("PEC {} has {} msgId", pecId, msgId);
                                        return postacert;
                                    })

//                                 Escludere questi daticert. Non avendo il msgid terminante con il dominio pago non sono state inviate
//                                 da noi
                                    .filter(endsWithDomainPredicate)

//                                 Daticert filtrati
                                    .doOnDiscard(Postacert.class, postacert -> {
                                        if (isPostaCertificataPredicate.test(postacert)) {
                                            log.debug("PEC {} discarded, is {}", pecId, POSTA_CERTIFICATA);
                                        } else if (!endsWithDomainPredicate.test(postacert)) {
                                            log.debug("PEC {} discarded, it was not sent by us", pecId);
                                        }
                                    })

//                                 Mono.zip contenente:
//                                 - Il daticert
//                                 - La chiamata al ms del gestore repository per reperire la richiesta
//                                 - La chiamata al ms PEC per reperire le info dell'ultimo evento di una richiesta (servirà per le
//                                 metriche custom)
//                                 - La chiamata ad Aruba per recuperare il messaggio della PEC
                                    .flatMap(postacert -> {
                                        var presaInCaricoInfo = decodeMessageId(postacert.getDati().getMsgid());
                                        var requestIdx = presaInCaricoInfo.getRequestIdx();
                                        var clientId = presaInCaricoInfo.getXPagopaExtchCxId();

                                        return Mono.zip(Mono.just(postacert),
                                                gestoreRepositoryCall.getRichiesta(clientId, requestIdx),
                                                statusPullService.pecPullService(requestIdx,
                                                        presaInCaricoInfo.getXPagopaExtchCxId()),
                                                arubaCall.getMessageId(createGetMessageIdRequest(pecId, false)));
                                    })

//                                 Validate status
                                    .flatMap(objects -> {
                                        Postacert postacert = objects.getT1();
                                        RequestDto requestDto = objects.getT2();
                                        LegalMessageSentDetails legalMessageSentDetails = objects.getT3();
                                        GetMessageIDResponse getMessageIDResponse = objects.getT4();

                                        var nextStatus =
                                                decodePecStatusToMachineStateStatus(postacert.getTipo()).getStatusTransactionTableCompliant();

                                        return callMacchinaStati.statusValidation(requestDto.getxPagopaExtchCxId(),
                                                        transactionProcessConfigurationProperties.pec(),
                                                        requestDto.getStatusRequest(),
                                                        nextStatus)
                                                .map(unused -> Tuples.of(postacert,
                                                        requestDto,
                                                        legalMessageSentDetails,
                                                        getMessageIDResponse,
                                                        nextStatus))
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

//                                 Pubblicazione metriche custom su CloudWatch
                                    .flatMap(objects -> {
                                        Postacert postacert = objects.getT1();
                                        RequestDto requestDto = objects.getT2();
                                        LegalMessageSentDetails legalMessageSentDetails = objects.getT3();
                                        GetMessageIDResponse getMessageIDResponse = objects.getT4();
                                        String nextStatus = objects.getT5();

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
                                                        requestDto,
                                                        getMessageIDResponse,
                                                        cloudWatchPecMetricsInfo,
                                                        nextStatus
                                                ));
                                    })

                                    //Preparazione payload per la coda stati PEC
                                    .flatMap(objects ->
                                    {
                                        Postacert postacert = objects.getT1();
                                        RequestDto requestDto = objects.getT2();
                                        GetMessageIDResponse getMessageIDResponse = objects.getT3();
                                        CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo = objects.getT4();
                                        String nextStatus = objects.getT5();

                                        var mimeMessage = getMimeMessage(getMessageIDResponse.getMessage());
                                        var pecIdMessageId = getMessageIdFromMimeMessage(mimeMessage);
                                        var requestIdx = requestDto.getRequestIdx();
                                        var xPagopaExtchCxId = requestDto.getxPagopaExtchCxId();
                                        var eventDetails = postacert.getErrore();
                                        var senderDigitalAddress = arubaSecretValue.getPecUsername();
                                        var senderDomain = getDomainFromAddress(senderDigitalAddress);
                                        var receiversDomain = getDomainFromAddress(getFromFromMimeMessage(mimeMessage)[0]);

                                        return generateLocation(requestIdx, xPagopaExtchCxId, attachBytes)
                                                .map(location ->
                                                {
                                                    var generatedMessageDto = createGeneratedMessageByStatus(receiversDomain,
                                                            senderDomain,
                                                            pecIdMessageId,
                                                            postacert.getTipo(),
                                                            location);

                                                    log.debug("PEC {} has {} requestId", pecId, requestIdx);

                                                    var digitalProgressStatusDto =
                                                            new DigitalProgressStatusDto().eventTimestamp(cloudWatchPecMetricsInfo.getNextEventTimestamp())
                                                                    .eventDetails(eventDetails)
                                                                    .generatedMessage(generatedMessageDto);

                                                    return NotificationTrackerQueueDto.builder()
                                                            .requestIdx(requestIdx)
                                                            .xPagopaExtchCxId(xPagopaExtchCxId)
                                                            .nextStatus(nextStatus)
                                                            .digitalProgressStatusDto(digitalProgressStatusDto)
                                                            .build();

                                                });

                                    })
//                                 Pubblicazione sulla coda degli stati PEC
                                    .flatMap(notificationTrackerQueueDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                            notificationTrackerQueueDto))

//                                 Return un Mono contenente il pecId
                                    .thenReturn(pecId)

//                                 Se per qualche motivo questo daticert è da escludere tornare comunque il pecId
                                    .switchIfEmpty(Mono.just(pecId))

//                                 Se avviene un errore all'interno di questa catena tornare un Mono.empty per non bloccare il flux
                                    .onErrorResume(throwable -> Mono.empty());
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

//         Error logging
                .doOnError(throwable -> {
                    if (throwable instanceof CallMacchinaStati.StatusValidationBadRequestException ||
                            throwable instanceof InvalidNextStatusException) {
                        log.debug(throwable.getMessage());
                    } else {
                        log.error(throwable.getMessage(), throwable);
                    }
                })

                .doOnComplete(() -> isScaricamentoEsitiPecRunning = false)

                .subscribe();
    }

    Mono<String> generateLocation(String requestIdx, String xPagopaExtchCxId, byte[] fileBytes) {

        FileCreationRequest fileCreationRequest = new FileCreationRequest().contentType(ContentTypes.APPLICATION_XML)
                .documentType(PN_EXTERNAL_LEGAL_FACTS.getValue())
                .status("");

        var checksumValue = generateSha256(fileBytes);

        return fileCall.postFile(xPagopaExtchCxId, "pn-external-channels_api_key", checksumValue, xPagopaExtchCxId + "~" + requestIdx, fileCreationRequest)
                .flatMap(fileCreationResponse ->
                {
                    String uploadUrl = fileCreationResponse.getUploadUrl();
                    log.info(uploadUrl);
                    return uploadWebClient.put()
                            .uri(URI.create(uploadUrl))
                            .header("Content-Type", ContentTypes.APPLICATION_XML)
                            .header("x-amz-meta-secret", fileCreationResponse.getSecret())
                            .header("x-amz-checksum-sha256", checksumValue)
                            .bodyValue(fileBytes)
                            .retrieve()
                            .toBodilessEntity()
                            .thenReturn(SAFESTORAGE_PREFIX + fileCreationResponse.getKey());
                });
    }

    private String generateSha256(byte[] fileBytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256");
            md.update(fileBytes);
            byte[] digest = md.digest();
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            throw new ShaGenerationException(e.getMessage());
        }
    }

}
