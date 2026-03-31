package it.pagopa.pn.ec.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.QueueOperationsService;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.dto.ses.SesNotificationDto;
import it.pagopa.pn.ec.email.model.dto.sns.SnsRawMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sqs.SqsTimeoutProvider;
import it.pagopa.pn.ec.util.LogSanitizer;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.Semaphore;

import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.SesEventsUtils.*;


@Service
@CustomLog
public class LavorazioneSesEventsService implements QueueOperationsService {
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final Semaphore semaphore;
    private final EmailSqsQueueName emailSqsQueueName;
    private LogSanitizer logSanitizer;
    private SqsTimeoutProvider sqsTimeoutProvider;
    private final ObjectMapper objectMapper;


    public LavorazioneSesEventsService(GestoreRepositoryCall gestoreRepositoryCall, EmailService emailService, SqsService sqsService, SesService sesService, NotificationTrackerSqsName notificationTrackerSqsName,
                                       LogSanitizer logSanitizer, SqsTimeoutProvider sqsTimeoutProvider, @Value("${lavorazione-email.max-thread-pool-size}") Integer maxThreadPoolSize,
                                       EmailSqsQueueName emailSqsQueueName, ObjectMapper objectMapper) {
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.sqsService = sqsService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.logSanitizer = logSanitizer;
        this.sqsTimeoutProvider = sqsTimeoutProvider;
        this.semaphore=new Semaphore(maxThreadPoolSize);
        this.emailSqsQueueName = emailSqsQueueName;
        this.objectMapper = objectMapper;
    }

    @SqsListener(value = "${sqs.queue.email.ses-events-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void lavorazioneSesEventsListener(final String rawMessage, final Acknowledgement acknowledgement) throws JsonProcessingException {
        String queueName=emailSqsQueueName.sesEventsName();
        log.info("lavorazioneSesEventsListener rawMessage: {}", rawMessage);
        SnsRawMessageDto snsMessage = objectMapper.readValue(rawMessage, SnsRawMessageDto.class);
        String sesPayload = snsMessage.getMessage();
        log.info("lavorazioneSesEventsListener rawMessage sesPayload: {} ", sesPayload);
        SesNotificationDto sesNotificationDto = objectMapper.readValue(rawMessage, SesNotificationDto.class);
        String messageId = sesNotificationDto.getMail() != null ? sesNotificationDto.getMail().getMessageId() : "null";
        log.info("Ricevuto evento SES: eventType={}, messageId={}", sesNotificationDto.getNotificationType() != null ? sesNotificationDto.getNotificationType() : "unknown", messageId);
        lavorazioneSesEvents(sesNotificationDto, queueName, acknowledgement)
                .doOnSuccess(result -> {acknowledgement.acknowledgeAsync();
                    log.logEndingProcess(LAVORAZIONE_SES_EVENT_EMAIL);
                })
                .doOnError(ex -> {log.logEndingProcess(LAVORAZIONE_SES_EVENT_EMAIL, false, ex.getMessage());})
                .onErrorResume(RepositoryManagerException.RequestNotFoundException.class, ex -> {
                    log.info("Message skipped caused by request with messageId={} not found", messageId);
                    return Mono.empty();
                })
                .block();
    }

    Mono<SendMessageResponse> lavorazioneSesEvents(SesNotificationDto sesNotificationDto, String queueName, Acknowledgement acknowledgement) {
        log.logStartingProcess(LAVORAZIONE_SES_EVENT_EMAIL);
        log.info("Start process {} for event {} into queue {}", LAVORAZIONE_SES_EVENT_EMAIL, sesNotificationDto.getNotificationType() != null ? sesNotificationDto.getNotificationType() : "unknown", emailSqsQueueName.sesEventsName());
        String eventType = sesNotificationDto.getNotificationType();
        String messageId = sesNotificationDto.getMail().getMessageId();
        return preliminaryChecks(sesNotificationDto, acknowledgement)
                .filter(Boolean::booleanValue)
                .flatMap(ignored -> Mono.defer(() -> {
                    try {
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    String status = mapSesEventToStatus(eventType);
                    return gestoreRepositoryCall.getRequestMetadataByMessageId(messageId)
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warn("Request non trovata per messageId={}", messageId);
                                return Mono.error(new RepositoryManagerException.RequestByMessageIdNotFoundException(messageId));
                            }))
                            .flatMap(requestDto -> {
                                PresaInCaricoInfo presaInCaricoInfo = buildPresaInCaricoInfo(requestDto);
                                return sendNotificationOnStatusQueue(
                                        presaInCaricoInfo,
                                        status,
                                        new DigitalProgressStatusDto());
                            });
                }))
                .doOnError(ex -> log.logEndingProcess(LAVORAZIONE_SES_EVENT_EMAIL, false, logSanitizer.sanitize(ex.getMessage())))
                .doOnSuccess(result -> log.logEndingProcess(LAVORAZIONE_SES_EVENT_EMAIL))
                .doFinally(signalType -> semaphore.release())
                .timeout(sqsTimeoutProvider.getTimeoutForQueue(queueName));

    }

    private PresaInCaricoInfo buildPresaInCaricoInfo(RequestDto requestDto) {

        PresaInCaricoInfo presaInCaricoInfo = new PresaInCaricoInfo();

        presaInCaricoInfo.setRequestIdx(requestDto.getRequestIdx());
        presaInCaricoInfo.setXPagopaExtchCxId(requestDto.getxPagopaExtchCxId());

        return presaInCaricoInfo;
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, status, digitalProgressStatusDto));
    }
}
