package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.ShaGenerationException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.pec.PnPostacert;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.ScaricamentoEsitiPecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchTransitionElapsedTimeMetricsInfo;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchTransitionElapsedTimeMetricsInfo;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pec.daticert.Destinatari;
import lombok.CustomLog;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.constant.DocumentType.PN_EXTERNAL_LEGAL_FACTS;
import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.consolidatore.utils.ContentTypes.MESSAGE_RFC822;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.decodeMessageId;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.*;


@CustomLog
@Service
public class LavorazioneEsitiPecService {

    private final SqsService sqsService;
    private final DaticertService daticertService;
    private final StatusPullService statusPullService;
    private final CloudWatchPecMetrics cloudWatchPecMetrics;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final ArubaSecretValue arubaSecretValue;
    private final FileCall fileCall;
    private final WebClient uploadWebClient;
    private final ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMacchinaStati;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    private final Semaphore semaphore;
    private static final String SAFESTORAGE_PREFIX = "safestorage://";

    public LavorazioneEsitiPecService(SqsService sqsService, DaticertService daticertService, StatusPullService statusPullService, CloudWatchPecMetrics cloudWatchPecMetrics, NotificationTrackerSqsName notificationTrackerSqsName, ArubaSecretValue arubaSecretValue, FileCall fileCall, WebClient uploadWebClient, ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties, GestoreRepositoryCall gestoreRepositoryCall, CallMacchinaStati callMacchinaStati, TransactionProcessConfigurationProperties transactionProcessConfigurationProperties, @Value("${lavorazione-esiti-pec.max-thread-pool-size}") Integer maxThreadPoolSize) {
        this.sqsService = sqsService;
        this.daticertService = daticertService;
        this.statusPullService = statusPullService;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.arubaSecretValue = arubaSecretValue;
        this.fileCall = fileCall;
        this.uploadWebClient = uploadWebClient;
        this.scaricamentoEsitiPecProperties = scaricamentoEsitiPecProperties;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMacchinaStati = callMacchinaStati;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
        this.semaphore = new Semaphore(maxThreadPoolSize);
    }

    @SqsListener(value = "${scaricamento-esiti-pec.sqs-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneEsitiPecInteractive(final RicezioneEsitiPecDto ricezioneEsitiPecDto, Acknowledgment acknowledgment) {
        logIncomingMessage(scaricamentoEsitiPecProperties.sqsQueueName(), ricezioneEsitiPecDto);
        lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment).subscribe();
    }

    Mono<Void> lavorazioneEsitiPec(final RicezioneEsitiPecDto ricezioneEsitiPecDto, Acknowledgment acknowledgment) {

        MDC.clear();

        var messageID = ricezioneEsitiPecDto.getMessageID();
        var message = ricezioneEsitiPecDto.getMessage();
        var mimeMessage = getMimeMessage(message);
        var daticert = findAttachmentByName(mimeMessage, "daticert.xml");
        var postacert = daticertService.getPostacertFromByteArray(daticert);
        var msgId = postacert.getDati().getMsgid();
        msgId = msgId.substring(1, msgId.length() - 1);
        var presaInCaricoInfo = decodeMessageId(msgId);

        MDC.put(MDC_CORR_ID_KEY, concatRequestId(presaInCaricoInfo.getXPagopaExtchCxId(), presaInCaricoInfo.getRequestIdx()));

        log.logStartingProcess(LAVORAZIONE_ESITI_PEC);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AtomicReference<String> requestIdx = new AtomicReference<>();

        return MDCUtils.addMDCToContextAndExecute(statusPullService.pecPullService(presaInCaricoInfo.getRequestIdx(), presaInCaricoInfo.getXPagopaExtchCxId())
                            .zipWhen(legalMessageSentDetails -> {
                                requestIdx.set(presaInCaricoInfo.getRequestIdx());
                                var clientId = presaInCaricoInfo.getXPagopaExtchCxId();
                                return gestoreRepositoryCall.getRichiesta(clientId, requestIdx.get());
                            })
                            //Pubblicazione metriche custom su CloudWatch
                            .flatMap(objects ->
                            {
                                LegalMessageSentDetails legalMessageSentDetails = objects.getT1();
                                RequestDto requestDto = objects.getT2();

                                //Se la validation è delivered->delivered, IGNORA STATUS VALIDATION E MARCA COME LETTO.
                                Destinatari destinatario = postacert.getIntestazione().getDestinatari().get(0);
                                var tipoDestinatario = destinatario.getTipo();

                                var nextStatus = "";
                                if (tipoDestinatario.equals(DESTINATARIO_ESTERNO)) {
                                    nextStatus = Status.NOT_PEC.getStatusTransactionTableCompliant();
                                } else {
                                    nextStatus = decodePecStatusToMachineStateStatus(postacert.getTipo()).getStatusTransactionTableCompliant();
                                }

                                var nextEventTimestamp = createTimestampFromDaticertDate(postacert.getDati().getData());
                                var cloudWatchPecMetricsInfo = CloudWatchTransitionElapsedTimeMetricsInfo.builder()
                                        .previousStatus(requestDto.getStatusRequest())
                                        .previousEventTimestamp(
                                                legalMessageSentDetails.getEventTimestamp())
                                        .nextStatus(nextStatus)
                                        .nextEventTimestamp(nextEventTimestamp)
                                        .build();

                                log.debug("Starting {} for PEC '{}'", PUBLISH_CUSTOM_PEC_METRICS, messageID);
                                return cloudWatchPecMetrics.publishTransitionElapsedTimeMetrics(cloudWatchPecMetricsInfo)
                                        .thenReturn(Tuples.of(requestDto,
                                                cloudWatchPecMetricsInfo,
                                                nextStatus));

                            })
                            //Preparazione payload per la coda stati PEC
                            .flatMap(objects -> {
                                RequestDto requestDto = objects.getT1();
                                CloudWatchTransitionElapsedTimeMetricsInfo cloudWatchTransitionElapsedTimeMetricsInfo = objects.getT2();
                                String nextStatus = objects.getT3();

                                log.debug(BUILDING_PEC_QUEUE_PAYLOAD, messageID, LAVORAZIONE_ESITI_PEC);

                                requestIdx.set(requestDto.getRequestIdx());
                                var xPagopaExtchCxId = requestDto.getxPagopaExtchCxId();
                                var eventDetails = postacert.getErrore();
                                var senderDigitalAddress = arubaSecretValue.getPecUsername();
                                var senderDomain = getDomainFromAddress(senderDigitalAddress);
                                var receiversDomain = ricezioneEsitiPecDto.getReceiversDomain();

                                return generateLocation(requestIdx.get(), message)
                                        .map(location ->
                                        {
                                            var generatedMessageDto = createGeneratedMessageByStatus(receiversDomain,
                                                    senderDomain,
                                                    messageID,
                                                    postacert.getTipo(),
                                                    location);

                                            var digitalProgressStatusDto =
                                                    new DigitalProgressStatusDto().eventTimestamp(cloudWatchTransitionElapsedTimeMetricsInfo.getNextEventTimestamp())
                                                            .eventDetails(eventDetails)
                                                            .generatedMessage(generatedMessageDto);

                                            return NotificationTrackerQueueDto.builder()
                                                    .requestIdx(requestIdx.get())
                                                    .xPagopaExtchCxId(xPagopaExtchCxId)
                                                    .nextStatus(nextStatus)
                                                    .digitalProgressStatusDto(digitalProgressStatusDto)
                                                    .build();
                                        });
                            })

                            //Pubblicazione sulla coda degli stati PEC
                            .flatMap(notificationTrackerQueueDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                    notificationTrackerQueueDto))
                .doOnSuccess(result -> {
                    log.logEndingProcess(LAVORAZIONE_ESITI_PEC);
                    acknowledgment.acknowledge();
                })
                .doOnError(throwable -> log.logEndingProcess(LAVORAZIONE_ESITI_PEC, false, throwable.getMessage()))
                .then()
                .doFinally(signalType -> semaphore.release()));
    }


    Mono<String> generateLocation(String requestIdx, byte[] fileBytes) {

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GENERATE_LOCATION, requestIdx);

        FileCreationRequest fileCreationRequest = new FileCreationRequest()
                .contentType(MESSAGE_RFC822)
                .documentType(PN_EXTERNAL_LEGAL_FACTS.getValue())
                .status("");

        var checksumValue = generateSha256(fileBytes);
        var xPagopaExtchCxId = scaricamentoEsitiPecProperties.clientHeaderValue();

        return fileCall.postFile(xPagopaExtchCxId, scaricamentoEsitiPecProperties.apiKeyHeaderValue(), checksumValue, xPagopaExtchCxId + "~" + requestIdx, fileCreationRequest)
                .flatMap(fileCreationResponse ->
                {
                    String uploadUrl = fileCreationResponse.getUploadUrl();
                    return uploadWebClient.put()
                            .uri(URI.create(uploadUrl))
                            .header("Content-Type", MESSAGE_RFC822)
                            .header("x-amz-meta-secret", fileCreationResponse.getSecret())
                            .header("x-amz-checksum-sha256", checksumValue)
                            .bodyValue(fileBytes)
                            .retrieve()
                            .toBodilessEntity()
                            .thenReturn(SAFESTORAGE_PREFIX + fileCreationResponse.getKey());
                }).doOnSuccess(location -> log.debug(SUCCESSFUL_OPERATION_LABEL, GENERATE_LOCATION, location));
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
