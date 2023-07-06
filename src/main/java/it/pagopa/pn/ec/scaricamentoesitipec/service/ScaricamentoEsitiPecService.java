package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.SemaphoreException;
import it.pagopa.pn.ec.commons.exception.ShaGenerationException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.consolidatore.utils.ContentTypes;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.ScaricamentoEsitiPecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchPecMetricsInfo;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pec.daticert.Destinatari;
import it.pec.daticert.Postacert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import static it.pagopa.pn.ec.commons.constant.DocumentType.PN_EXTERNAL_LEGAL_FACTS;
import static it.pagopa.pn.ec.commons.service.impl.DatiCertServiceImpl.createTimestampFromDaticertDate;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.decodeMessageId;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.ScaricamentoEsitiPecUtils.*;

@Slf4j
@Service
public class ScaricamentoEsitiPecService {

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

    public ScaricamentoEsitiPecService(SqsService sqsService, DaticertService daticertService, StatusPullService statusPullService, CloudWatchPecMetrics cloudWatchPecMetrics, NotificationTrackerSqsName notificationTrackerSqsName, ArubaSecretValue arubaSecretValue, FileCall fileCall, WebClient uploadWebClient, ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties, GestoreRepositoryCall gestoreRepositoryCall, CallMacchinaStati callMacchinaStati, TransactionProcessConfigurationProperties transactionProcessConfigurationProperties, @Value("${scaricamento-esiti-pec.max-thread-pool-size}") Integer maxThreadPoolSize) {
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

        log.info("<-- START LAVORAZIONE ESITI PEC -->");

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new SemaphoreException(e.getMessage());
        }

        return Mono.just(ricezioneEsitiPecDto)
                .flatMap(ricEsitiPecDto ->
                {

                    var messageID = ricEsitiPecDto.getMessageID();
                    var daticert = ricEsitiPecDto.getDaticert();

                    log.debug("---> LAVORAZIONE ESITI PEC - LAVORAZIONE MESSAGGIO <--- MessageID : {} , Daticert : {}", messageID, new String(daticert));

                    return Mono.just(daticertService.getPostacertFromByteArray(daticert))
                            .flatMap(postacert -> {

                                var msgId = postacert.getDati().getMsgid();
                                msgId = msgId.substring(1, msgId.length() - 1);

                                var presaInCaricoInfo = decodeMessageId(msgId);
                                var requestIdx = presaInCaricoInfo.getRequestIdx();
                                var clientId = presaInCaricoInfo.getXPagopaExtchCxId();

                                log.debug("LAVORAZIONE ESITI PEC - PEC messageId - clientId is {}, requestId is {}, messageID is {}", clientId, requestIdx, messageID);

                                return statusPullService.pecPullService(requestIdx, clientId)
                                        .map(legalMessageSentDetails -> Tuples.of(postacert, legalMessageSentDetails, presaInCaricoInfo));
                            })
                            .flatMap(objects -> {

                                Postacert postacert = objects.getT1();
                                LegalMessageSentDetails legalMessageSentDetails = objects.getT2();
                                PresaInCaricoInfo presaInCaricoInfo = objects.getT3();

                                var requestIdx = presaInCaricoInfo.getRequestIdx();
                                var clientId = presaInCaricoInfo.getXPagopaExtchCxId();

                                log.debug("LAVORAZIONE ESITI PEC - GET RICHIESTA - clientId is {}, requestId is {}, messageID is {}", clientId, requestIdx, messageID);

                                return gestoreRepositoryCall.getRichiesta(clientId, requestIdx)
                                        .map(requestDto -> Tuples.of(postacert, legalMessageSentDetails, requestDto));

                            })
                            //Pubblicazione metriche custom su CloudWatch
                            .flatMap(objects ->
                            {
                                Postacert postacert = objects.getT1();
                                LegalMessageSentDetails legalMessageSentDetails = objects.getT2();
                                RequestDto requestDto = objects.getT3();

                                //Se la validation è delivered->delivered, IGNORA STATUS VALIDATION E MARCA COME LETTO.
                                Destinatari destinatario = postacert.getIntestazione().getDestinatari().get(0);
                                var tipoDestinatario = destinatario.getTipo();

                                var nextStatus = "";
                                if (tipoDestinatario.equals(DESTINATARIO_ESTERNO)) {
                                    nextStatus = Status.NOT_PEC.getStatusTransactionTableCompliant();
                                } else {
                                    nextStatus = decodePecStatusToMachineStateStatus(postacert.getTipo()).getStatusTransactionTableCompliant();
                                }

                                log.debug("---> LAVORAZIONE ESITI PEC - PUBLISH CUSTOM CLOUD WATCH METRICS <--- MessageID : {}", messageID);

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
                            .flatMap(objects -> {
                                Postacert postacert = objects.getT1();
                                RequestDto requestDto = objects.getT2();
                                CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo = objects.getT3();
                                String nextStatus = objects.getT4();

                                log.debug("---> LAVORAZIONE ESITI PEC - BUILDING PEC QUEUE PAYLOAD <--- MessageID : {}", messageID);

                                var requestIdx = requestDto.getRequestIdx();
                                var xPagopaExtchCxId = requestDto.getxPagopaExtchCxId();
                                var eventDetails = postacert.getErrore();
                                var senderDigitalAddress = arubaSecretValue.getPecUsername();
                                var senderDomain = getDomainFromAddress(senderDigitalAddress);
                                var receiversDomain = ricEsitiPecDto.getReceiversDomain();

                                return generateLocation(requestIdx, daticert)
                                        .map(location ->
                                        {
                                            var generatedMessageDto = createGeneratedMessageByStatus(receiversDomain,
                                                    senderDomain,
                                                    messageID,
                                                    postacert.getTipo(),
                                                    location);

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

                            //Pubblicazione sulla coda degli stati PEC
                            .flatMap(notificationTrackerQueueDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                    notificationTrackerQueueDto));
                })
                .doOnSuccess(result -> acknowledgment.acknowledge())
                .then()
                .doFinally(signalType -> semaphore.release());
    }


    Mono<String> generateLocation(String requestIdx, byte[] fileBytes) {

        log.debug("---> LAVORAZIONE ESITI PEC - START GENERATING LOCATION <--- RequestId: {}", requestIdx);

        FileCreationRequest fileCreationRequest = new FileCreationRequest().contentType(ContentTypes.APPLICATION_XML)
                .documentType(PN_EXTERNAL_LEGAL_FACTS.getValue())
                .status("");

        var checksumValue = generateSha256(fileBytes);
        var xPagopaExtchCxId = scaricamentoEsitiPecProperties.clientHeaderValue();

        return fileCall.postFile(xPagopaExtchCxId, scaricamentoEsitiPecProperties.apiKeyHeaderValue(), checksumValue, xPagopaExtchCxId + "~" + requestIdx, fileCreationRequest)
                .flatMap(fileCreationResponse ->
                {
                    String uploadUrl = fileCreationResponse.getUploadUrl();
                    log.debug("---> LAVORAZIONE ESITI PEC - UPLOADING FILE USING URL {} <--- ", uploadUrl);
                    return uploadWebClient.put()
                            .uri(URI.create(uploadUrl))
                            .header("Content-Type", ContentTypes.APPLICATION_XML)
                            .header("x-amz-meta-secret", fileCreationResponse.getSecret())
                            .header("x-amz-checksum-sha256", checksumValue)
                            .bodyValue(fileBytes)
                            .retrieve()
                            .toBodilessEntity()
                            .thenReturn(SAFESTORAGE_PREFIX + fileCreationResponse.getKey());
                }).doOnSuccess(location -> log.debug("---> LAVORAZIONE ESITI PEC - LOCATION GENERATED : {} <--- ", location));
    }

    private String generateSha256(byte[] fileBytes) {
        log.info("---> LAVORAZIONE ESITI PEC - GENERATING SHA256 FROM FILE WITH LENGTH {} <---", fileBytes.length);
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
