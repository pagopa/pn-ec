package it.pagopa.pn.ec.sms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.service.SnsService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;
import static java.time.LocalTime.now;

@Service
@Slf4j
public class SmsService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final SnsService snsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SmsSqsQueueName smsSqsQueueName;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    private String idSaved;


    protected SmsService(AuthService authService, SqsService sqsService, SnsService snsService,
                         GestoreRepositoryCall gestoreRepositoryCall, NotificationTrackerSqsName notificationTrackerSqsName,
                         SmsSqsQueueName smsSqsQueueName,
                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.smsSqsQueueName = smsSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

        var smsPresaInCaricoInfo = (SmsPresaInCaricoInfo) presaInCaricoInfo;

        var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();
        digitalCourtesySmsRequest.setRequestId(smsPresaInCaricoInfo.getRequestIdx());

//      Insert request from SMS request and publish to Notification Tracker with next status -> BOOKED
        return insertRequestFromSms(digitalCourtesySmsRequest, smsPresaInCaricoInfo.getXPagopaExtchCxId()).then(sqsService.send(
                                                                                                                  notificationTrackerSqsName.statoSmsName(),
                                                                                                                  createNotificationTrackerQueueDtoDigital(smsPresaInCaricoInfo,
                                                                                                                                                           transactionProcessConfigurationProperties.smsStartStatus(),
                                                                                                                                                           "booked",
                                                                                                                                                           new DigitalProgressStatusDto())))
//                                                                                                        Publish to SMS INTERACTIVE or
//                                                                                                        SMS BATCH
                                                                                                          .flatMap(sendMessageResponse -> {
                                                                                                              DigitalCourtesySmsRequest.QosEnum
                                                                                                                      qos =
                                                                                                                      smsPresaInCaricoInfo.getDigitalCourtesySmsRequest()
                                                                                                                                          .getQos();
                                                                                                              if (qos == INTERACTIVE) {
                                                                                                                  return sqsService.send(
                                                                                                                          smsSqsQueueName.interactiveName(),
                                                                                                                          smsPresaInCaricoInfo);
                                                                                                              } else if (qos == BATCH) {
                                                                                                                  return sqsService.send(
                                                                                                                          smsSqsQueueName.batchName(),
                                                                                                                          smsPresaInCaricoInfo);
                                                                                                              } else {
                                                                                                                  return Mono.empty();
                                                                                                              }
                                                                                                          }).then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromSms(final DigitalCourtesySmsRequest digitalCourtesySmsRequest, String xPagopaExtchCxId) {
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();

            requestDto.setRequestIdx(digitalCourtesySmsRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalCourtesySmsRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
            digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesySmsRequest.getQos().name()));
            digitalRequestPersonalDto.setReceiverDigitalAddress(digitalCourtesySmsRequest.getReceiverDigitalAddress());
            digitalRequestPersonalDto.setMessageText(digitalCourtesySmsRequest.getMessageText());
            digitalRequestPersonalDto.setSenderDigitalAddress(digitalCourtesySmsRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setSubjectText("");
            requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
            digitalRequestMetadataDto.setCorrelationId(digitalCourtesySmsRequest.getCorrelationId());
            digitalRequestMetadataDto.setEventType(digitalCourtesySmsRequest.getEventType());
            digitalRequestMetadataDto.setTags(digitalCourtesySmsRequest.getTags());
            digitalRequestMetadataDto.setMessageContentType(PLAIN);
            digitalRequestMetadataDto.setChannel(SMS);
            requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = "${sqs.queue.sms.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void lavorazioneRichiestaInteractive(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        logIncomingMessage(smsSqsQueueName.interactiveName(), smsPresaInCaricoInfo);
        lavorazioneRichiesta(smsPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${cron.value.lavorazione-batch-sms}")
    void lavorazioneRichiestaBatch() {
        log.info("<-- START LAVORAZIONE RICHIESTA SMS BATCH -->");
        sqsService.getOneMessage(smsSqsQueueName.batchName(), SmsPresaInCaricoInfo.class)
                  .doOnNext(smsPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(smsSqsQueueName.batchName(),
                                                                                        smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(smsPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                             lavorazioneRichiesta(smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(smsPresaInCaricoInfoSqsMessageWrapper.getT1(),
                                                                                                      smsSqsQueueName.batchName()))
                  .map(MonoResultWrapper::new)
                  .defaultIfEmpty(new MonoResultWrapper<>(null))
                  .repeat()
                  .takeWhile(MonoResultWrapper::isNotEmpty)
                  .subscribe();
    }

    public Mono<SendMessageResponse> lavorazioneRichiesta(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {

//      Try to send SMS
        return snsService.send(smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(),
                               smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getMessageText())

//                       Retry to send SMS if fails
                         .retryWhen(DEFAULT_RETRY_STRATEGY)

//                        Set message id after send
                         .map(this::createGeneratedMessageDto)

//                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                         .flatMap(generatedMessageDto -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                                                         createNotificationTrackerQueueDtoDigital(smsPresaInCaricoInfo,
                                                                                                                  "booked",
                                                                                                                  "sent",
                                                                                                                  new DigitalProgressStatusDto().generatedMessage(
                                                                                                                          generatedMessageDto)))

//                                                                An error occurred during SQS publishing to the Notification Tracker ->
//                                                                Publish to Errori SMS queue and notify to retry update status only
//                                                                TODO: CHANGE THE PAYLOAD
                                                                   .onErrorResume(SqsPublishException.class,
                                                                                  sqsPublishException -> sqsService.send(smsSqsQueueName.errorName(),
                                                                                                                         smsPresaInCaricoInfo)))

//                       The maximum number of retries has ended
                         .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                                        snsMaxRetriesExceeded -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                                                                 createNotificationTrackerQueueDtoDigital(
                                                                                         smsPresaInCaricoInfo,
                                                                                         "booked",
                                                                                         "retry",
                                                                                         new DigitalProgressStatusDto()))

//                                                                         Publish to ERRORI SMS queue
                                                                           .then(sqsService.send(smsSqsQueueName.errorName(),
                                                                                                 smsPresaInCaricoInfo)));
    }

    private GeneratedMessageDto createGeneratedMessageDto(PublishResponse publishResponse) {
        return new GeneratedMessageDto().id(publishResponse.messageId()).system("toBeDefined");
    }

    //@Scheduled(cron = "0 */1 * * * *")
    /*public void test(){
        log.info("test scheduled");
    }*/


    @Scheduled(cron = "${cron.value.gestione-retry-sms}")
    void gestioneRetrySmsScheduler() {
        log.info("<-- START GESTIONE RETRY SMS-->");
        idSaved = null;
        sqsService.getOneMessage(smsSqsQueueName.errorName(), SmsPresaInCaricoInfo.class)
                .doOnNext(smsPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(smsSqsQueueName.errorName(),
                        smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(smsPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                        gestioneRetrySms(smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent(), smsPresaInCaricoInfoSqsMessageWrapper.getMessage())))
                /*.flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(smsPresaInCaricoInfoSqsMessageWrapper.getT1(),
                        smsSqsQueueName.errorName()))*/
                .map(MonoResultWrapper::new)
                .defaultIfEmpty(new MonoResultWrapper<>(null))
                .repeat()
                .takeWhile(MonoResultWrapper::isNotEmpty)
                .subscribe();
        /*sqsService.getAllQueueMessage(smsSqsQueueName.errorName(), SmsPresaInCaricoInfo.class)
                .doOnNext(smsPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(smsSqsQueueName.batchName(),
                        smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(smsPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                        gestioneRetrySms(smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent(), smsPresaInCaricoInfoSqsMessageWrapper.getMessage())))
                *//*.flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(smsPresaInCaricoInfoSqsMessageWrapper.getT1(),
                        smsSqsQueueName.batchName()))*//*
                .subscribe();*/
    }
    /*void gestioneRetrySmsScheduler() {
        log.info("<-- START GESTIONE RETRY SMS-->");
        sqsService.getAllQueueMessage(smsSqsQueueName.errorName(), 5).flatMap(message -> {
            try {
                return Mono.just(objectMapper.readValue(message.body(), SmsPresaInCaricoInfo.class))
                        .doOnNext(smsPresaInCaricoInfo -> log.info(smsPresaInCaricoInfo.toString()))
                        .flatMap(smsPresaInCaricoInfo -> gestioneRetrySms(smsPresaInCaricoInfo, message))
                        .thenReturn(message);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });*///.flatMap(message -> sqsService.deleteMessageFromQueue(message, smsSqsQueueName.batchName())).subscribe();

    public Mono<SendMessageResponse> gestioneRetrySms(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, Message message) {

        log.info("<-- START GESTIONE ERRORI SMS -->");
        logIncomingMessage(smsSqsQueueName.errorName(), smsPresaInCaricoInfo);

    File file = new File("src/main/resources/commons/retryPolicy.json");
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, List<BigDecimal>> retryPolicies;

    {
        try {
            retryPolicies = objectMapper.readValue(file, new TypeReference<Map<String, List<BigDecimal>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//        log.info(String.valueOf(retryPolicies.get(0).contains("SMS")));

        var requestId = smsPresaInCaricoInfo.getRequestIdx();
        var clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();

        return gestoreRepositoryCall.getRichiesta(requestId)
                /*.filter(requestDto -> !Objects.equals(requestDto.getStatusRequest(), "toDelete"))
                .doOnError(throwable -> {
                })*/
                .map(requestDto -> {
                    if(Objects.equals(requestDto.getStatusRequest(), "toDelete")){
                        requestDto.setStatusRequest("Deleted");
                        PatchDto patchDto = new PatchDto();
                        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                        gestoreRepositoryCall.patchRichiesta(requestId, patchDto);
                        log.info("Il messaggio è stato rimosso dalla coda d'errore per stato toDelete: {}", smsSqsQueueName.errorName());
                    }
                    return requestDto;
                })
                .filter(requestDto -> {
                    if(requestDto.getRequestMetadata().getRetry() != null) {
                        if (requestDto.getRequestMetadata().getRetry().getRetryStep().compareTo(BigDecimal.valueOf(3)) > 0) {
                            // operazioni per la rimozione del messaggio
                            log.info("Il messaggio è stato rimosso dalla coda d'errore per eccessivi tentativi: {}", smsSqsQueueName.errorName());
                            sqsService.deleteMessageFromQueue(message, smsSqsQueueName.errorName());
                            return false; // il messaggio è stato rimosso, quindi si deve terminare il flusso
                        }
                    }
                    return true;
                })
                .filter(requestDto -> !Objects.equals(requestDto.getRequestIdx(), idSaved))
                .flatMap(requestDto ->  {
                    if(requestDto.getRequestMetadata().getRetry() == null) {
                        log.info("Primo tentativo di Retry");
                        RetryDto retryDto = new RetryDto();
                        retryDto.setRetryPolicy(retryPolicies.get("SMS"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                        requestDto.getRequestMetadata().setRetry(retryDto);

                    } else {
                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                        log.info(retryNumber + " tentativo di Retry");
                    }

                    PatchDto patchDto = new PatchDto();
                    patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

                    return gestoreRepositoryCall.patchRichiesta(requestId, patchDto);
                })
                .filter(requestDto -> {

                    var dateTime1 = requestDto.getRequestMetadata().getRetry().getLastRetryTimestamp();
                    var dateTime2 = OffsetDateTime.now();
                    Duration duration = Duration.between(dateTime1, dateTime2);
                    int step = requestDto.getRequestMetadata().getRetry().getRetryStep().intValueExact();
                    long minutes = duration.toMinutes();
                    long minutesToCheck = requestDto.getRequestMetadata().getRetry().getRetryPolicy().get(step).longValue();
                    return minutes >= minutesToCheck;
                })
                .flatMap(requestDto -> {
                    requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now());
                    requestDto.getRequestMetadata().getRetry().setRetryStep(requestDto.getRequestMetadata().getRetry().getRetryStep().add(BigDecimal.ONE));
                    PatchDto patchDto = new PatchDto();
                    patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                    return gestoreRepositoryCall.patchRichiesta(requestId, patchDto);
                })
                .flatMap(requestDto -> {
                    log.info("requestDto Value:", requestDto.getRequestMetadata().getRetry());

                    return snsService.send(smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(),
                                    smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getMessageText())

//                       Retry to send SMS if fails
                            .retryWhen(DEFAULT_RETRY_STRATEGY)

//                        Set message id after send
                            .map(this::createGeneratedMessageDto)

//                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                            .flatMap(generatedMessageDto -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                            createNotificationTrackerQueueDtoDigital(smsPresaInCaricoInfo,
                                                    "booked",
                                                    "sent",
                                                    new DigitalProgressStatusDto().generatedMessage(
                                                            generatedMessageDto)))

//                                                                An error occurred during SQS publishing to the Notification Tracker ->
//                                                                Publish to Errori SMS queue and notify to retry update status only
//                                                                TODO: CHANGE THE PAYLOAD
                                    .onErrorResume(sqsPublishException -> {
                                        if(idSaved == null){
                                            idSaved = requestId;
                                        }
                                        return sqsService.send(smsSqsQueueName.errorName(), smsPresaInCaricoInfo);
                                            }
                                    )
                            )

//                       The maximum number of retries has ended
                            .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                                    snsMaxRetriesExceeded -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                                    createNotificationTrackerQueueDtoDigital(
                                                            smsPresaInCaricoInfo,
                                                            "booked",
                                                            "retry",
                                                            new DigitalProgressStatusDto()))
                            )
//                          In case of success, message removed from error queue
                            .doOnSuccess(result -> {
                                log.info("Il messaggio è stato gestito correttamente e rimosso dalla coda d'errore", smsSqsQueueName.errorName());
                                sqsService.deleteMessageFromQueue(message, smsSqsQueueName.errorName());
                            });
                });
    }
}
