package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
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
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pec.bridgews.*;
import it.pec.daticert.Destinatari;
import it.pec.daticert.Postacert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Random;
import java.util.function.Predicate;

import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.DOMAIN;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.decodeMessageId;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.createGeneratedMessageByStatus;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.decodePecStatusToMachineStateStatus;

@Slf4j
public class ScaricamentoEsitiPecService {
    @Autowired
    private SqsService sqsService;
    @Autowired
    private ArubaCall arubaCall;
    @Autowired
    private DaticertService daticertService;
    @Autowired
    private CallMacchinaStati callMacchinaStati;
    @Autowired
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Autowired
    private StatusPullService statusPullService;
    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @Autowired
    private CloudWatchPecMetrics cloudWatchPecMetrics;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @Autowired
    private ArubaSecretValue arubaSecretValue;
    @Autowired
    private Random random;
    @Value("${scaricamento-esiti-pec.get-messages.limit}")
    private String scaricamentoEsitiPecGetMessagesLimit;

    private static final String DESTINATARIO_ESTERNO = "esterno";

    private GetMessageID createGetMessageIdRequest(String pecId, boolean markSeen) {
        var getMessageID = new GetMessageID();
        getMessageID.setMailid(pecId);
        getMessageID.setIsuid(1);
        getMessageID.setMarkseen(markSeen ? 1 : 0);
        return getMessageID;
    }

    @SqsListener(value = "${sqs.queue.pec.ricezione-esiti-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneEsitiPec(final RicezioneEsitiPecDto ricezioneEsitiPecDto, final Acknowledgment acknowledgment) {
        lavorazioneEsitiPec(ricezioneEsitiPecDto).doOnSuccess(result -> acknowledgment.acknowledge()).subscribe();
    }

    Mono<String> lavorazioneEsitiPec(RicezioneEsitiPecDto ricezioneEsitiPecDto) {

        log.info("<-- START LAVORAZIONE ESITI PEC -->");

        var mimeMessage = getMimeMessage(ricezioneEsitiPecDto.getMessage());
        var messageID = getMessageIdFromMimeMessage(mimeMessage);

        return Mono.just(ricezioneEsitiPecDto.getDaticert())
                .flatMap(daticert -> {

                    var postacert = daticertService.getPostacertFromByteArray(daticert);

                    var presaInCaricoInfo = decodeMessageId(postacert.getDati().getMsgid());
                    var requestIdx = presaInCaricoInfo.getRequestIdx();
                    var clientId = presaInCaricoInfo.getXPagopaExtchCxId();

                    log.debug("PEC messageId - clientId is {}, requestId is {}", clientId, requestIdx);

                    return Mono.zip(Mono.just(postacert),
                            gestoreRepositoryCall.getRichiesta(clientId, requestIdx),
                            statusPullService.pecPullService(requestIdx,
                                    presaInCaricoInfo.getXPagopaExtchCxId()));
                })

                //Validate status
                .flatMap(objects -> {
                    Postacert postacert = objects.getT1();

                    Destinatari destinatario = postacert.getIntestazione().getDestinatari().get(0);
                    var tipoDestinatario = destinatario.getTipo();

                    RequestDto requestDto = objects.getT2();
                    LegalMessageSentDetails legalMessageSentDetails = objects.getT3();

                    var nextStatus = "";
                    if (tipoDestinatario.equals(DESTINATARIO_ESTERNO)) {
                        nextStatus = Status.NOT_PEC.getStatusTransactionTableCompliant();
                    } else {
                        nextStatus = decodePecStatusToMachineStateStatus(postacert.getTipo()).getStatusTransactionTableCompliant();
                    }
                    String finalNextStatus = nextStatus;
                    return callMacchinaStati.statusValidation(requestDto.getxPagopaExtchCxId(),
                                    transactionProcessConfigurationProperties.pec(),
                                    requestDto.getStatusRequest(),
                                    nextStatus)
                            .map(unused -> Tuples.of(postacert,
                                    requestDto,
                                    legalMessageSentDetails,
                                    finalNextStatus))
                            .doOnError(CallMacchinaStati.StatusValidationBadRequestException.class,
                                    throwable -> log.debug(
                                            "La chiamata al notification tracker della PEC {} " +
                                                    "associata alla richiesta {} ha tornato 400 come " +
                                                    "status",
                                            messageID,
                                            requestDto.getRequestIdx()))
                            .doOnError(InvalidNextStatusException.class,
                                    throwable -> log.debug(
                                            "La PEC {} associata alla richiesta {} ha " +
                                                    "comunicato i propri" + " esiti in " +
                                                    "un ordine non corretto al notification tracker",
                                            messageID,
                                            requestDto.getRequestIdx()));
                })

                //Pubblicazione metriche custom su CloudWatch
                .flatMap(objects -> {
                    Postacert postacert = objects.getT1();
                    RequestDto requestDto = objects.getT2();
                    LegalMessageSentDetails legalMessageSentDetails = objects.getT3();
                    String nextStatus = objects.getT4();

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
                                    cloudWatchPecMetricsInfo,
                                    nextStatus));
                })

                //Preparazione payload per la coda stati PEC
                .map(objects -> {
                    Postacert postacert = objects.getT1();
                    RequestDto requestDto = objects.getT2();
                    CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo = objects.getT3();
                    String nextStatus = objects.getT4();

                    var pecIdMessageId = getMessageIdFromMimeMessage(mimeMessage);
                    var requestIdx = requestDto.getRequestIdx();
                    var xPagopaExtchCxId = requestDto.getxPagopaExtchCxId();
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

                    log.debug("PEC {} has {} requestId", pecIdMessageId, requestIdx);


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
                })

                //Pubblicazione sulla coda degli stati PEC
                .flatMap(notificationTrackerQueueDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                        notificationTrackerQueueDto))

                //Return un Mono contenente il messageId
                .thenReturn(messageID)

                //Se per qualche motivo questo daticert è da escludere tornare comunque il pecId
                .switchIfEmpty(Mono.just(messageID))

                //         Error logging
                .doOnError(throwable -> {
                    if (throwable instanceof CallMacchinaStati.StatusValidationBadRequestException ||
                            throwable instanceof InvalidNextStatusException) {
                        log.debug(throwable.getMessage());
                    } else {
                        log.error(throwable.getMessage(), throwable);
                    }
                })

                //Se avviene un errore all'interno di questa catena tornare un Mono.empty per non bloccare il flux
                .onErrorResume(throwable -> Mono.empty())
                .doOnSuccess(unused -> log.info("---> LAVORAZIONE ESITI PEC ENDED <---"));
    }

}
